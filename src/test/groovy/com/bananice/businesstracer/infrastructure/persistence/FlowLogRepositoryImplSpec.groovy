package com.bananice.businesstracer.infrastructure.persistence

import com.bananice.businesstracer.TestApplication
import com.bananice.businesstracer.domain.model.FlowLog
import com.bananice.businesstracer.domain.repository.FlowLogRepository
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
class FlowLogRepositoryImplSpec extends Specification {

    @Resource
    @Subject
    FlowLogRepository flowLogRepository

    @Shared
    def now = LocalDateTime.now()

    // ==================== Helper Methods ====================

    private FlowLog buildFlowLog(Map overrides = [:]) {
        def defaults = [
                flowCode  : "default-flow",
                name      : "Default Flow",
                businessId: "bus-default",
                status    : "IN_PROGRESS",
                createTime: now
        ]
        def merged = defaults + overrides
        FlowLog.builder()
                .flowCode(merged.flowCode as String)
                .name(merged.name as String)
                .businessId(merged.businessId as String)
                .status(merged.status as String)
                .createTime(merged.createTime as LocalDateTime)
                .build()
    }

    // ==================== Save & Exists ====================

    def "保存FlowLog后能通过flowCode和businessId判断存在"() {
        given: "构建一条FlowLog记录"
        def flowLog = buildFlowLog(flowCode: "flow-exists", businessId: "bus-exists")

        when: "保存记录"
        flowLogRepository.save(flowLog)

        then: "existsBy检查返回true"
        flowLogRepository.existsByFlowCodeAndBusinessId("flow-exists", "bus-exists")
    }

    def "不存在的flowCode和businessId组合返回false"() {
        expect: "未保存的组合不存在"
        !flowLogRepository.existsByFlowCodeAndBusinessId("no-flow", "no-bus")
    }

    // ==================== FindAll & Count ====================

    def "保存FlowLog后能分页查询出来"() {
        given: "保存一条FlowLog"
        def flowLog = buildFlowLog(flowCode: "flow-find", businessId: "bus-find", name: "Test Flow")
        flowLogRepository.save(flowLog)

        when: "分页查询"
        def results = flowLogRepository.findAll("flow-find", "bus-find", 1, 10)
        def total = flowLogRepository.count("flow-find", "bus-find")

        then: "返回1条记录，总数为1"
        results.size() == 1
        total == 1L
        verifyAll(results[0]) {
            flowCode == "flow-find"
            businessId == "bus-find"
            status == "IN_PROGRESS"
        }
    }

    def "查询不存在的flowCode应返回空列表"() {
        expect: "不存在的条件返回空结果"
        flowLogRepository.findAll("non-existent-flow", null, 1, 10).isEmpty()
        flowLogRepository.count("non-existent-flow", null) == 0L
    }

    def "分页查询能正确分页"() {
        given: "保存5条同flowCode的记录"
        (1..5).each { i ->
            flowLogRepository.save(
                    buildFlowLog(flowCode: "flow-page", businessId: "bus-page-${i}")
            )
        }

        expect: "分页大小为2时，第1页返回2条，第3页返回1条"
        flowLogRepository.findAll("flow-page", null, 1, 2).size() == 2
        flowLogRepository.findAll("flow-page", null, 2, 2).size() == 2
        flowLogRepository.findAll("flow-page", null, 3, 2).size() == 1

        and: "总数始终为5"
        flowLogRepository.count("flow-page", null) == 5L
    }

    // ==================== Update Status ====================

    def "通过flowCode和businessId更新状态"() {
        given: "保存一条IN_PROGRESS状态的记录"
        flowLogRepository.save(buildFlowLog(flowCode: "flow-upd", businessId: "bus-upd"))

        when: "更新状态为COMPLETED"
        flowLogRepository.updateStatus("flow-upd", "bus-upd", "COMPLETED")
        def result = flowLogRepository.findAll("flow-upd", "bus-upd", 1, 10)

        then: "状态已变为COMPLETED"
        result[0].status == "COMPLETED"
    }

    def "通过businessId批量更新状态"() {
        given: "保存两条同businessId、不同flowCode的记录"
        def businessId = "bus-batch"
        flowLogRepository.save(buildFlowLog(flowCode: "flow-batch-1", businessId: businessId))
        flowLogRepository.save(buildFlowLog(flowCode: "flow-batch-2", businessId: businessId))

        when: "通过businessId将所有记录状态改为FAILED"
        flowLogRepository.updateStatusByBusinessId(businessId, "FAILED")

        then: "两条记录的状态都变为FAILED"
        def results = flowLogRepository.findAll(null, businessId, 1, 10)
        results.size() == 2
        results.every { it.status == "FAILED" }
    }

    // ==================== Status Transition (Data Table) ====================

    @Unroll
    def "FlowLog状态从 #fromStatus 更新为 #toStatus"() {
        given: "保存一条初始状态的记录"
        def flowCode = "flow-trans-${fromStatus}-${toStatus}"
        flowLogRepository.save(
                buildFlowLog(flowCode: flowCode, businessId: "bus-trans", status: fromStatus)
        )

        when: "更新状态"
        flowLogRepository.updateStatus(flowCode, "bus-trans", toStatus)
        def result = flowLogRepository.findAll(flowCode, "bus-trans", 1, 10)

        then: "状态已正确变更"
        result[0].status == toStatus

        where: "不同的状态流转场景"
        fromStatus    | toStatus
        "IN_PROGRESS" | "COMPLETED"
        "IN_PROGRESS" | "FAILED"
        "COMPLETED"   | "FAILED"
    }

    // ==================== Isolation ====================

    def "不同flowCode和businessId的记录互不干扰"() {
        given: "保存多条不同组合的记录"
        flowLogRepository.save(buildFlowLog(flowCode: "flow-iso-1", businessId: "bus-iso-1"))
        flowLogRepository.save(buildFlowLog(flowCode: "flow-iso-2", businessId: "bus-iso-2"))

        expect: "各条件查询互不影响"
        flowLogRepository.findAll("flow-iso-1", "bus-iso-1", 1, 10).size() == 1
        flowLogRepository.findAll("flow-iso-2", "bus-iso-2", 1, 10).size() == 1
        flowLogRepository.findAll("flow-iso-1", "bus-iso-2", 1, 10).isEmpty()
    }

    def "updateStatusByBusinessId不会影响其他businessId的记录"() {
        given: "保存两条不同businessId的记录"
        flowLogRepository.save(buildFlowLog(flowCode: "flow-safe", businessId: "bus-target"))
        flowLogRepository.save(buildFlowLog(flowCode: "flow-safe", businessId: "bus-other"))

        when: "只更新一个businessId的状态"
        flowLogRepository.updateStatusByBusinessId("bus-target", "FAILED")

        then: "目标记录状态变更"
        flowLogRepository.findAll("flow-safe", "bus-target", 1, 10)[0].status == "FAILED"

        and: "其他记录不受影响"
        flowLogRepository.findAll("flow-safe", "bus-other", 1, 10)[0].status == "IN_PROGRESS"
    }
}
