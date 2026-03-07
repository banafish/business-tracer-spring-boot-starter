package com.bananice.businesstracer.infrastructure.aspect

import com.bananice.businesstracer.TestApplication
import com.bananice.businesstracer.domain.model.NodeLog
import com.bananice.businesstracer.domain.repository.NodeLogRepository
import com.bananice.businesstracer.domain.repository.DetailLogRepository
import com.bananice.businesstracer.fixture.TracedTestService
import com.bananice.businesstracer.infrastructure.context.TraceContextHolder
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification
import spock.lang.Subject
import spock.util.concurrent.PollingConditions

import javax.annotation.Resource

@SpringBootTest(classes = TestApplication)
@Transactional
class BusinessTraceAspectSpec extends Specification {

    @Resource
    @Subject
    TracedTestService tracedTestService

    @Resource
    NodeLogRepository nodeLogRepository

    @Resource
    DetailLogRepository detailLogRepository

    PollingConditions conditions = new PollingConditions(timeout: 3, initialDelay: 0.2)

    def cleanup() {
        TraceContextHolder.clear()
    }

    // ==================== Helper ====================

    private List<NodeLog> findLogs(String businessId) {
        nodeLogRepository.findByBusinessId(businessId)
    }

    // ==================== 基础场景 ====================

    def "基本@BusinessTrace方法执行后自动记录NodeLog"() {
        when: "调用带@BusinessTrace注解的方法"
        def result = tracedTestService.basicMethod("ORD-001")

        then: "方法正常返回"
        result == "ok-ORD-001"

        and: "数据库中有对应的NodeLog"
        conditions.eventually {
            def logs = findLogs("ORD-001")
            assert logs.size() == 1
            verifyAll(logs[0]) {
                businessId == "ORD-001"
                code == "BASIC_NODE"
                name == "BASIC_NODE"   // name未设置时默认等于code
                status == "COMPLETED"
                costTime != null
                costTime >= 0
                nodeId != null
                traceId != null
            }
        }
    }

    def "执行完毕后TraceContext被清理"() {
        when:
        tracedTestService.basicMethod("ORD-CLEAN")

        then: "上下文已被清除"
        TraceContextHolder.getContext() == null
    }

    // ==================== 完整参数场景 ====================

    def "完整注解参数——name、operation、inputParams、outputParams全部正确记录"() {
        when:
        def result = tracedTestService.fullMethod("BIZ-FULL")

        then: "方法返回正确"
        result == "result-BIZ-FULL"

        and: "NodeLog中记录了完整信息"
        conditions.eventually {
            def logs = findLogs("BIZ-FULL")
            assert logs.size() == 1
            verifyAll(logs[0]) {
                businessId == "BIZ-FULL"
                code == "FULL_NODE"
                name == "完整节点"
                content == "执行完整操作"   // operation
                inputParams == "BIZ-FULL"   // SpEL #bizId
                outputParams == "result-BIZ-FULL"  // SpEL #result
                status == "COMPLETED"
            }
        }
    }

    // ==================== 异常场景 ====================

    def "方法抛出异常时NodeLog状态为FAILED并记录异常信息"() {
        when: "调用会抛异常的方法"
        tracedTestService.throwingMethod("ERR-001")

        then: "异常被正常抛出"
        def ex = thrown(RuntimeException)
        ex.message.contains("测试异常: ERR-001")

        and: "NodeLog记录了FAILED状态和异常信息"
        conditions.eventually {
            def logs = findLogs("ERR-001")
            assert logs.size() == 1
            verifyAll(logs[0]) {
                businessId == "ERR-001"
                code == "ERROR_NODE"
                status == "FAILED"
                exception != null
                exception.contains("测试异常: ERR-001")
            }
        }
    }

    // ==================== 手动recordError场景 ====================

    def "调用BusinessTracer.recordError后NodeLog状态为FAILED"() {
        when:
        def result = tracedTestService.manualErrorMethod("MANUAL-001")

        then: "方法本身没有抛异常，正常返回"
        result == "done"

        and: "NodeLog状态被标记为FAILED（因为recordError设置了errorRecorded标志）"
        conditions.eventually {
            def logs = findLogs("MANUAL-001")
            assert logs.size() == 1
            assert logs[0].status == "FAILED"
            assert logs[0].exception == null  // 没有实际异常抛出
    
            // and: "DetailLog中记录了手动的错误日志"
            def details = detailLogRepository.findByParentNodeId(logs[0].nodeId)
            assert details.size() == 1
            assert details[0].status == "FAILED"
            assert details[0].content == "手动记录的错误: MANUAL-001"
        }
    }

    // ==================== 嵌套调用场景 ====================

    def "嵌套@BusinessTrace调用形成父子NodeLog关系"() {
        when: "调用外层方法（外层 -> 内层）"
        def result = tracedTestService.outerMethod("NEST-001")

        then: "返回内层结果"
        result == "inner-NEST-001"

        and: "数据库中有两条NodeLog且关系正确"
        conditions.eventually {
            def logs = findLogs("NEST-001")
            assert logs.size() == 2
    
            def outerLog = logs.find { it.code == "OUTER_NODE" }
            def innerLog = logs.find { it.code == "INNER_NODE" }
            assert outerLog != null
            assert innerLog != null
    
            assert innerLog.parentNodeId == outerLog.nodeId
            assert outerLog.traceId == innerLog.traceId
            assert outerLog.parentNodeId == null
        }
    }

    // ==================== appName ====================

    def "NodeLog中记录了spring.application.name"() {
        when:
        tracedTestService.basicMethod("APP-NAME-TEST")

        then:
        conditions.eventually {
            def logs = findLogs("APP-NAME-TEST")
            assert !logs.isEmpty()
            assert logs[0].appName != null
        }
    }

    // ==================== content 默认方法名 ====================

    def "operation为空时content默认为方法名"() {
        when:
        tracedTestService.basicMethod("CONTENT-DEFAULT")

        then:
        conditions.eventually {
            def logs = findLogs("CONTENT-DEFAULT")
            assert !logs.isEmpty()
            assert logs[0].content == "basicMethod"
        }
    }

    // ==================== costTime ====================

    def "NodeLog记录了合理的costTime"() {
        when:
        tracedTestService.basicMethod("COST-TIME")

        then: "costTime >= 0 且是毫秒级"
        conditions.eventually {
            def logs = findLogs("COST-TIME")
            assert !logs.isEmpty()
            assert logs[0].costTime >= 0
            assert logs[0].costTime < 5000  // 不应超过5秒
        }
    }

    // ==================== 异步上下文传播场景 ====================

    def "通过businessTracerTaskExecutor执行的异步任务能继承TraceContext"() {
        when: "调用包含异步任务的方法"
        def result = tracedTestService.spawnTask("ASYNC-001")

        then: "方法正常返回"
        result == "spawned"

        and: "主节点的NodeLog被记录"
        conditions.eventually {
            def logs = findLogs("ASYNC-001")
            assert logs.size() == 1
            assert logs[0].code == "PARENT_ASYNC_NODE"

            // 异步线程中的DetailLog也能正确关联到该NodeLog
            def details = detailLogRepository.findByParentNodeId(logs[0].nodeId)
            assert details.size() == 1
            assert details[0].content == "async-log-from-ASYNC-001"
            assert details[0].businessId == "ASYNC-001"
        }
    }

    // ==================== 多次调用互不干扰 ====================

    def "多次调用同一方法生成独立的NodeLog"() {
        when:
        tracedTestService.basicMethod("MULTI-001")
        tracedTestService.basicMethod("MULTI-002")

        then: "每个businessId各一条"
        conditions.eventually {
            def logs1 = findLogs("MULTI-001")
            def logs2 = findLogs("MULTI-002")
            assert logs1.size() == 1
            assert logs2.size() == 1
            assert logs1[0].nodeId != logs2[0].nodeId
        }
    }
}
