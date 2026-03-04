package com.bananice.businesstracer.infrastructure.persistence

import com.bananice.businesstracer.TestApplication
import com.bananice.businesstracer.domain.model.NodeLog
import com.bananice.businesstracer.domain.repository.NodeLogRepository
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import javax.annotation.Resource
import java.time.LocalDateTime

@SpringBootTest(classes = TestApplication)
@Transactional
class NodeLogRepositoryImplSpec extends Specification {

    @Resource
    @Subject
    NodeLogRepository nodeLogRepository

    @Shared
    def now = LocalDateTime.now()

    // ==================== Helper Methods ====================

    private NodeLog buildNodeLog(Map overrides = [:]) {
        def defaults = [
                businessId  : "bus-default",
                code        : "defaultNode",
                name        : "Default Node",
                traceId     : "trace-default",
                nodeId      : "node-default",
                parentNodeId: null,
                content     : "default content",
                appName     : "test-app",
                status      : "COMPLETED",
                costTime    : 100L,
                inputParams : "{}",
                outputParams: "{}",
                createTime  : now
        ]
        def merged = defaults + overrides
        NodeLog.builder()
                .businessId(merged.businessId as String)
                .code(merged.code as String)
                .name(merged.name as String)
                .traceId(merged.traceId as String)
                .nodeId(merged.nodeId as String)
                .parentNodeId(merged.parentNodeId as String)
                .content(merged.content as String)
                .appName(merged.appName as String)
                .status(merged.status as String)
                .costTime(merged.costTime as Long)
                .inputParams(merged.inputParams as String)
                .outputParams(merged.outputParams as String)
                .createTime(merged.createTime as LocalDateTime)
                .build()
    }

    // ==================== Save & Query ====================

    def "保存NodeLog后能通过businessId查询到"() {
        given: "构建一条NodeLog记录"
        def nodeLog = buildNodeLog(businessId: "bus-12345", code: "testNode", name: "Test Node")

        when: "保存并查询"
        nodeLogRepository.save(nodeLog)
        def results = nodeLogRepository.findByBusinessId("bus-12345")

        then: "查询结果与保存数据一致"
        results.size() == 1
        verifyAll(results[0]) {
            businessId == "bus-12345"
            code == "testNode"
            name == "Test Node"
            status == "COMPLETED"
            costTime == 100L
        }
    }

    def "查询不存在的businessId应返回空列表"() {
        expect: "不存在的businessId返回空列表"
        nodeLogRepository.findByBusinessId("non-existent-id").isEmpty()
    }

    // ==================== Multiple Records ====================

    def "同一businessId下保存多条记录能全部查出"() {
        given: "同一businessId下保存3条记录"
        def businessId = "bus-multi"
        (1..3).each { i ->
            nodeLogRepository.save(
                    buildNodeLog(businessId: businessId, nodeId: "node-${i}", code: "node${i}")
            )
        }

        when: "按businessId查询"
        def results = nodeLogRepository.findByBusinessId(businessId)

        then: "返回全部3条记录"
        results.size() == 3

        and: "每条记录的nodeId不重复"
        results*.nodeId.toSet().size() == 3
    }

    def "不同businessId的记录互不干扰"() {
        given: "分别保存两个不同businessId的记录"
        nodeLogRepository.save(buildNodeLog(businessId: "bus-A", nodeId: "node-A"))
        nodeLogRepository.save(buildNodeLog(businessId: "bus-B", nodeId: "node-B"))

        expect: "各自只能查到自己的记录"
        nodeLogRepository.findByBusinessId("bus-A").size() == 1
        nodeLogRepository.findByBusinessId("bus-B").size() == 1
        nodeLogRepository.findByBusinessId("bus-A")[0].nodeId == "node-A"
        nodeLogRepository.findByBusinessId("bus-B")[0].nodeId == "node-B"
    }

    // ==================== Data Table Parameterized Tests ====================

    @Unroll
    def "保存状态为 #status 的NodeLog能正确持久化"() {
        given: "构建指定状态的NodeLog"
        def nodeLog = buildNodeLog(
                businessId: "bus-status-${status}",
                status: status,
                costTime: costTime
        )

        when: "保存并查询"
        nodeLogRepository.save(nodeLog)
        def result = nodeLogRepository.findByBusinessId("bus-status-${status}")

        then: "状态和耗时正确持久化"
        result[0].status == status
        result[0].costTime == costTime

        where: "不同的状态和耗时组合"
        status      | costTime
        "COMPLETED" | 50L
        "FAILED"    | 200L
        "COMPLETED" | 0L
    }

    @Unroll
    def "NodeLog可以保存 #desc 的异常信息"() {
        given: "构建带异常信息的NodeLog"
        def nodeLog = buildNodeLog(businessId: "bus-ex-${index}", code: "exNode")
        nodeLog.exception = exceptionMsg

        when: "保存并查询"
        nodeLogRepository.save(nodeLog)
        def result = nodeLogRepository.findByBusinessId("bus-ex-${index}")

        then: "异常信息正确持久化"
        result[0].exception == exceptionMsg

        where: "不同的异常场景"
        desc     | exceptionMsg                        | index
        "null"   | null                                | 1
        "空字符串"  | ""                                  | 2
        "长堆栈信息" | "java.lang.NullPointerException\n" +
                   "\tat com.example.Foo.bar(Foo.java:42)" | 3
    }

    // ==================== Edge Cases ====================

    def "NodeLog的inputParams和outputParams支持JSON字符串持久化"() {
        given: "构建包含复杂JSON参数的NodeLog"
        def inputJson = '{"name":"Alice","age":30,"tags":["vip","test"]}'
        def outputJson = '{"result":"success","data":{"id":1}}'
        def nodeLog = buildNodeLog(
                businessId: "bus-json",
                inputParams: inputJson,
                outputParams: outputJson
        )

        when: "保存并查询"
        nodeLogRepository.save(nodeLog)
        def result = nodeLogRepository.findByBusinessId("bus-json")

        then: "JSON参数原样保存"
        verifyAll(result[0]) {
            inputParams == inputJson
            outputParams == outputJson
        }
    }

    def "NodeLog的parentNodeId可以为null（根节点场景）"() {
        given: "构建parentNodeId为null的NodeLog"
        def nodeLog = buildNodeLog(businessId: "bus-root", parentNodeId: null)

        when: "保存并查询"
        nodeLogRepository.save(nodeLog)
        def result = nodeLogRepository.findByBusinessId("bus-root")

        then: "parentNodeId为null"
        result[0].parentNodeId == null
    }
}
