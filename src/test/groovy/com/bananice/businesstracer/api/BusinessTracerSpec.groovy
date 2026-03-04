package com.bananice.businesstracer.api

import com.bananice.businesstracer.domain.model.DetailLog
import com.bananice.businesstracer.domain.repository.DetailLogRepository
import com.bananice.businesstracer.infrastructure.context.TraceContext
import com.bananice.businesstracer.infrastructure.context.TraceContextHolder
import spock.lang.Specification

class BusinessTracerSpec extends Specification {

    DetailLogRepository mockRepository = Mock()

    def setup() {
        // 通过构造函数注入静态repository
        new BusinessTracer(mockRepository)
    }

    def cleanup() {
        TraceContextHolder.clear()
    }

    // ==================== Helper ====================

    private void setupContext(String businessId = "bus-001", String nodeId = "node-001") {
        TraceContextHolder.setContext(
                TraceContext.builder()
                        .businessId(businessId)
                        .nodeId(nodeId)
                        .traceId("trace-001")
                        .build()
        )
    }

    // ==================== record() ====================

    def "record()在有上下文时保存NORMAL状态的DetailLog"() {
        given: "设置TraceContext"
        setupContext("bus-record", "node-record")

        when: "调用record"
        BusinessTracer.record("测试日志内容")

        then: "保存一条NORMAL的DetailLog"
        1 * mockRepository.save({ DetailLog log ->
            log.businessId == "bus-record" &&
            log.parentNodeId == "node-record" &&
            log.content == "测试日志内容" &&
            log.status == "NORMAL" &&
            log.createTime != null
        })
    }

    def "record()在无上下文时不调用save"() {
        given: "不设置TraceContext"
        TraceContextHolder.clear()

        when:
        BusinessTracer.record("无上下文日志")

        then: "不应调用save"
        0 * mockRepository.save(_)
    }

    // ==================== recordError() ====================

    def "recordError()保存FAILED状态的DetailLog并标记errorRecorded"() {
        given: "设置TraceContext"
        setupContext("bus-err", "node-err")

        when: "调用recordError"
        BusinessTracer.recordError("出现异常")

        then: "保存一条FAILED的DetailLog"
        1 * mockRepository.save({ DetailLog log ->
            log.businessId == "bus-err" &&
            log.parentNodeId == "node-err" &&
            log.content == "出现异常" &&
            log.status == "FAILED"
        })

        and: "TraceContext的errorRecorded标记为true"
        TraceContextHolder.getContext().isErrorRecorded()
    }

    def "recordError()在无上下文时不调用save也不抛异常"() {
        given:
        TraceContextHolder.clear()

        when:
        BusinessTracer.recordError("无上下文错误")

        then: "不应调用save"
        0 * mockRepository.save(_)
        noExceptionThrown()
    }

    // ==================== Multiple Calls ====================

    def "同一上下文中多次调用record都会分别保存"() {
        given:
        setupContext()

        when: "连续调用3次"
        BusinessTracer.record("日志1")
        BusinessTracer.record("日志2")
        BusinessTracer.record("日志3")

        then: "save被调用3次"
        3 * mockRepository.save(_ as DetailLog)
    }

    def "先record再recordError，errorRecorded应为true"() {
        given:
        setupContext()

        when:
        BusinessTracer.record("普通日志")
        BusinessTracer.recordError("错误日志")

        then: "按顺序保存"
        1 * mockRepository.save({ it.status == "NORMAL" })

        then: "然后保存错误日志"
        1 * mockRepository.save({ it.status == "FAILED" })

        and: "errorRecorded标记为true"
        TraceContextHolder.getContext().isErrorRecorded()
    }
}
