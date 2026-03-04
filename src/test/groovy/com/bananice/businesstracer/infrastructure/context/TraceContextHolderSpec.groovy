package com.bananice.businesstracer.infrastructure.context

import org.slf4j.MDC
import spock.lang.Specification
import spock.lang.Subject

class TraceContextHolderSpec extends Specification {

    @Subject
    Class<TraceContextHolder> holder = TraceContextHolder

    def cleanup() {
        TraceContextHolder.clear()
    }

    // ==================== Basic Operations ====================

    def "setContext后getContext能取到相同对象"() {
        given: "构建一个TraceContext"
        def context = TraceContext.builder()
                .businessId("bus-001")
                .traceId("trace-001")
                .nodeId("node-001")
                .code("CREATE_ORDER")
                .name("创建订单")
                .build()

        when: "设置上下文"
        TraceContextHolder.setContext(context)

        then: "获取的上下文与设置的一致"
        TraceContextHolder.getContext() == context
        TraceContextHolder.getContext().businessId == "bus-001"
        TraceContextHolder.getContext().traceId == "trace-001"
    }

    def "未设置context时getContext返回null"() {
        expect:
        TraceContextHolder.getContext() == null
    }

    def "clear后getContext返回null"() {
        given: "先设置一个context"
        TraceContextHolder.setContext(
                TraceContext.builder().businessId("bus-001").traceId("trace-001").build()
        )

        when: "清除上下文"
        TraceContextHolder.clear()

        then:
        TraceContextHolder.getContext() == null
    }

    // ==================== MDC Integration ====================

    def "setContext时自动将businessId和traceId写入MDC"() {
        when: "设置包含businessId和traceId的上下文"
        TraceContextHolder.setContext(
                TraceContext.builder().businessId("bus-mdc").traceId("trace-mdc").build()
        )

        then: "MDC中能取到对应值"
        MDC.get(TraceContextHolder.MDC_BUSINESS_ID) == "bus-mdc"
        MDC.get(TraceContextHolder.MDC_TRACE_ID) == "trace-mdc"
    }

    def "clear后MDC中的businessId和traceId也被清除"() {
        given: "先设置context"
        TraceContextHolder.setContext(
                TraceContext.builder().businessId("bus-mdc").traceId("trace-mdc").build()
        )

        when: "清除"
        TraceContextHolder.clear()

        then: "MDC已清空"
        MDC.get(TraceContextHolder.MDC_BUSINESS_ID) == null
        MDC.get(TraceContextHolder.MDC_TRACE_ID) == null
    }

    // ==================== Convenience Methods ====================

    def "getBusinessId便捷方法返回当前context的businessId"() {
        given:
        TraceContextHolder.setContext(
                TraceContext.builder().businessId("bus-conv").build()
        )

        expect:
        TraceContextHolder.getBusinessId() == "bus-conv"
    }

    def "getNodeId便捷方法返回当前context的nodeId"() {
        given:
        TraceContextHolder.setContext(
                TraceContext.builder().nodeId("node-conv").build()
        )

        expect:
        TraceContextHolder.getNodeId() == "node-conv"
    }

    def "无context时getBusinessId和getNodeId返回null"() {
        expect:
        TraceContextHolder.getBusinessId() == null
        TraceContextHolder.getNodeId() == null
    }

    // ==================== Context Replacement ====================

    def "setContext可以覆盖之前的context"() {
        given: "先设置context1"
        def context1 = TraceContext.builder().businessId("bus-1").nodeId("node-1").build()
        TraceContextHolder.setContext(context1)

        when: "用context2覆盖"
        def context2 = TraceContext.builder().businessId("bus-2").nodeId("node-2").build()
        TraceContextHolder.setContext(context2)

        then: "取到的是context2"
        TraceContextHolder.getContext() == context2
        TraceContextHolder.getBusinessId() == "bus-2"
        TraceContextHolder.getNodeId() == "node-2"
    }

    // ==================== Thread Isolation ====================

    def "不同线程的TraceContext互相隔离"() {
        given: "主线程设置context"
        TraceContextHolder.setContext(
                TraceContext.builder().businessId("main-thread").build()
        )

        when: "在子线程中检查context"
        def childBusinessId = null
        def thread = new Thread({
            childBusinessId = TraceContextHolder.getBusinessId()
        })
        thread.start()
        thread.join()

        then: "子线程取不到主线程的context"
        childBusinessId == null

        and: "主线程的context不受影响"
        TraceContextHolder.getBusinessId() == "main-thread"
    }

    // ==================== ErrorRecorded Flag ====================

    def "TraceContext的errorRecorded默认为false"() {
        given:
        def context = TraceContext.builder().businessId("bus-err").build()

        expect:
        !context.isErrorRecorded()
    }

    def "设置errorRecorded为true后能正确读取"() {
        given:
        def context = TraceContext.builder().businessId("bus-err").build()
        TraceContextHolder.setContext(context)

        when: "标记错误"
        context.setErrorRecorded(true)

        then: "从Holder取出的context反映了错误状态"
        TraceContextHolder.getContext().isErrorRecorded()
    }
}
