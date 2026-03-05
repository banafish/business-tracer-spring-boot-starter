package com.bananice.businesstracer.application

import com.bananice.businesstracer.domain.model.DslConfig
import com.bananice.businesstracer.domain.model.DslNode
import com.bananice.businesstracer.domain.model.FlowLog
import com.bananice.businesstracer.domain.model.NodeLog
import com.bananice.businesstracer.domain.repository.FlowLogRepository
import com.bananice.businesstracer.domain.repository.NodeLogRepository
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.time.LocalDateTime

class FlowLogServiceSpec extends Specification {

    FlowLogRepository flowLogRepository = Mock()
    NodeLogRepository nodeLogRepository = Mock()
    DslService dslService = Mock()

    @Subject
    FlowLogService flowLogService = new FlowLogService(flowLogRepository, nodeLogRepository, dslService)

    // ==================== Helper ====================

    private DslConfig buildDsl(String flowCode, List<DslNode> nodes = []) {
        def dsl = new DslConfig()
        dsl.flowCode = flowCode
        dsl.name = "Flow $flowCode"
        dsl.nodes = nodes
        dsl
    }

    private DslNode buildDslNode(String code, Boolean isEndNode = false, List<DslNode> children = null) {
        def node = new DslNode()
        node.code = code
        node.isEndNode = isEndNode
        node.children = children
        node
    }

    private FlowLog buildFlowLog(String flowCode, String businessId, String status = "IN_PROGRESS") {
        FlowLog.builder()
                .flowCode(flowCode)
                .name("Flow $flowCode")
                .businessId(businessId)
                .status(status)
                .createTime(LocalDateTime.now())
                .build()
    }

    // ==================== recordFlowLogs ====================

    @Unroll
    def "recordFlowLogs在 #desc 时直接返回不做任何操作"() {
        when:
        flowLogService.recordFlowLogs(nodeCode, businessId)

        then: "不应调用任何service或repository"
        0 * dslService._
        0 * flowLogRepository._

        where:
        desc                | nodeCode | businessId
        "nodeCode为null"    | null     | "bus-1"
        "businessId为null"  | "CODE_A" | null
        "二者都为null"       | null     | null
    }

    def "recordFlowLogs找到匹配的DSL后调用saveIfNotExists"() {
        given: "有一个包含目标nodeCode的DSL"
        def dsl = buildDsl("flow-match", [buildDslNode("TARGET_NODE")])
        dslService.findDslsByNodeCode("TARGET_NODE") >> [dsl]
        flowLogRepository.existsByFlowCodeAndBusinessId("flow-match", "bus-1") >> false

        when:
        flowLogService.recordFlowLogs("TARGET_NODE", "bus-1")

        then: "保存FlowLog"
        1 * flowLogRepository.save({ FlowLog log ->
            log.flowCode == "flow-match" &&
            log.businessId == "bus-1" &&
            log.status == "IN_PROGRESS"
        })
    }

    def "recordFlowLogs已存在的flowLog不重复保存"() {
        given:
        def dsl = buildDsl("flow-dup")
        dslService.findDslsByNodeCode("NODE_A") >> [dsl]
        flowLogRepository.existsByFlowCodeAndBusinessId("flow-dup", "bus-1") >> true

        when:
        flowLogService.recordFlowLogs("NODE_A", "bus-1")

        then: "不应调用save"
        0 * flowLogRepository.save(_)
    }

    // ==================== saveIfNotExists ====================

    @Unroll
    def "saveIfNotExists在 #desc 时不操作"() {
        when:
        flowLogService.saveIfNotExists(flowCode, "Test", businessId)

        then:
        0 * flowLogRepository._

        where:
        desc               | flowCode   | businessId
        "flowCode为null"   | null       | "bus-1"
        "businessId为null" | "flow-123" | null
    }

    def "saveIfNotExists在repository抛异常时不传播异常(防御性编程)"() {
        given:
        flowLogRepository.existsByFlowCodeAndBusinessId(_, _) >> { throw new RuntimeException("DB Error") }

        when:
        flowLogService.saveIfNotExists("flow-err", "Test", "bus-err")

        then: "不抛异常"
        noExceptionThrown()
    }

    // ==================== queryFlowLogs ====================

    def "queryFlowLogs返回分页结果"() {
        given:
        def logs = [buildFlowLog("flow-q", "bus-q")]
        flowLogRepository.findAll("flow-q", "bus-q", 1, 10) >> logs
        flowLogRepository.count("flow-q", "bus-q") >> 1L

        when:
        def result = flowLogService.queryFlowLogs("flow-q", "bus-q", 1, 10)

        then:
        result.total == 1L
        result.pageNum == 1
        result.pageSize == 10
        result.list.size() == 1
    }

    def "queryFlowLogs无数据时返回空列表"() {
        given:
        flowLogRepository.findAll(_, _, _, _) >> []
        flowLogRepository.count(_, _) >> 0L

        when:
        def result = flowLogService.queryFlowLogs(null, null, 1, 10)

        then:
        result.total == 0L
        result.list.isEmpty()
    }

    // ==================== markFlowsAsFailed ====================

    def "markFlowsAsFailed调用repository更新状态"() {
        when:
        flowLogService.markFlowsAsFailed("bus-fail")

        then:
        1 * flowLogRepository.updateStatusByBusinessId("bus-fail", "FAILED")
    }

    def "markFlowsAsFailed传入null不调用repository"() {
        when:
        flowLogService.markFlowsAsFailed(null)

        then:
        0 * flowLogRepository._
    }

    // ==================== checkAndUpdateFlowStatus ====================

    def "所有endNode都已执行时更新状态为COMPLETED"() {
        given: "DSL定义了一个endNode"
        def endNode = buildDslNode("END_NODE", true)
        def dsl = buildDsl("flow-complete", [buildDslNode("START"), endNode])
        dslService.getDslByFlowCode("flow-complete") >> dsl

        and: "FlowLog存在且状态为IN_PROGRESS"
        flowLogRepository.findAll("flow-complete", "bus-comp", 1, 1) >> [
                buildFlowLog("flow-complete", "bus-comp", "IN_PROGRESS")
        ]

        and: "endNode已经执行"
        nodeLogRepository.findByBusinessId("bus-comp") >> [
                NodeLog.builder().code("END_NODE").businessId("bus-comp").build()
        ]

        when:
        flowLogService.checkAndUpdateFlowStatus("bus-comp", "flow-complete")

        then: "更新为COMPLETED"
        1 * flowLogRepository.updateStatus("flow-complete", "bus-comp", "COMPLETED")
    }

    def "endNode未全部执行时不更新状态"() {
        given:
        def dsl = buildDsl("flow-pending", [
                buildDslNode("END_A", true),
                buildDslNode("END_B", true)
        ])
        dslService.getDslByFlowCode("flow-pending") >> dsl
        flowLogRepository.findAll("flow-pending", "bus-pend", 1, 1) >> [
                buildFlowLog("flow-pending", "bus-pend")
        ]

        and: "只执行了END_A，未执行END_B"
        nodeLogRepository.findByBusinessId("bus-pend") >> [
                NodeLog.builder().code("END_A").businessId("bus-pend").build()
        ]

        when:
        flowLogService.checkAndUpdateFlowStatus("bus-pend", "flow-pending")

        then: "不更新状态"
        0 * flowLogRepository.updateStatus(_, _, _)
    }

    def "FlowLog已经是FAILED状态时不检查endNode"() {
        given:
        flowLogRepository.findAll("flow-failed", "bus-failed", 1, 1) >> [
                buildFlowLog("flow-failed", "bus-failed", "FAILED")
        ]

        when:
        flowLogService.checkAndUpdateFlowStatus("bus-failed", "flow-failed")

        then: "不调用dslService也不更新"
        0 * dslService.getDslByFlowCode(_)
        0 * flowLogRepository.updateStatus(_, _, _)
    }

    def "FlowLog不存在时不做任何操作"() {
        given:
        flowLogRepository.findAll("flow-none", "bus-none", 1, 1) >> []

        when:
        flowLogService.checkAndUpdateFlowStatus("bus-none", "flow-none")

        then:
        0 * dslService._
        0 * flowLogRepository.updateStatus(_, _, _)
    }

    // ==================== checkAndUpdateFlowStatusByNodeCode ====================

    @Unroll
    def "checkAndUpdateFlowStatusByNodeCode在 #desc 时不操作"() {
        when:
        flowLogService.checkAndUpdateFlowStatusByNodeCode(businessId, nodeCode)

        then:
        0 * dslService._

        where:
        desc               | businessId | nodeCode
        "nodeCode为null"   | "bus-1"    | null
        "businessId为null" | null       | "CODE_A"
    }

    def "checkAndUpdateFlowStatusByNodeCode遍历所有匹配DSL"() {
        given: "两个包含同一nodeCode的DSL"
        def dsl1 = buildDsl("flow-1", [buildDslNode("COMMON_NODE", true)])
        def dsl2 = buildDsl("flow-2", [buildDslNode("COMMON_NODE", true)])
        dslService.findDslsByNodeCode("COMMON_NODE") >> [dsl1, dsl2]

        and: "每个flow都有对应的FlowLog"
        flowLogRepository.findAll("flow-1", "bus-check", 1, 1) >> [buildFlowLog("flow-1", "bus-check")]
        flowLogRepository.findAll("flow-2", "bus-check", 1, 1) >> [buildFlowLog("flow-2", "bus-check")]

        and: "DSL配置"
        dslService.getDslByFlowCode("flow-1") >> dsl1
        dslService.getDslByFlowCode("flow-2") >> dsl2

        and: "endNode已执行"
        nodeLogRepository.findByBusinessId("bus-check") >> [
                NodeLog.builder().code("COMMON_NODE").businessId("bus-check").build()
        ]

        when:
        flowLogService.checkAndUpdateFlowStatusByNodeCode("bus-check", "COMMON_NODE")

        then: "两个flow都被更新"
        1 * flowLogRepository.updateStatus("flow-1", "bus-check", "COMPLETED")
        1 * flowLogRepository.updateStatus("flow-2", "bus-check", "COMPLETED")
    }
}
