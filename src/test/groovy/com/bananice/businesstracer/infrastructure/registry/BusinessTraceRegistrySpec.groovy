package com.bananice.businesstracer.infrastructure.registry

import spock.lang.Specification
import spock.lang.Subject

class BusinessTraceRegistrySpec extends Specification {

    @Subject
    BusinessTraceRegistry registry = new BusinessTraceRegistry()

    // ==================== Register & Query ====================

    def "注册节点后能通过code查到"() {
        when: "注册一个节点"
        registry.register("CREATE_ORDER", "创建订单")

        then: "能获取到节点信息"
        registry.hasNode("CREATE_ORDER")
        verifyAll(registry.getNode("CREATE_ORDER")) {
            code == "CREATE_ORDER"
            name == "创建订单"
        }
    }

    def "未注册的节点hasNode返回false"() {
        expect:
        !registry.hasNode("NOT_EXISTS")
    }

    def "未注册的节点getNode返回null"() {
        expect:
        registry.getNode("NOT_EXISTS") == null
    }

    // ==================== getAllNodes ====================

    def "初始状态getAllNodes返回空集合"() {
        expect:
        registry.getAllNodes().isEmpty()
    }

    def "注册多个节点后getAllNodes能全部返回"() {
        given: "注册3个节点"
        registry.register("NODE_A", "节点A")
        registry.register("NODE_B", "节点B")
        registry.register("NODE_C", "节点C")

        when:
        def allNodes = registry.getAllNodes()

        then:
        allNodes.size() == 3
        allNodes*.code.toSet() == ["NODE_A", "NODE_B", "NODE_C"] as Set
    }

    // ==================== Overwrite ====================

    def "相同code重复注册会覆盖之前的name"() {
        given: "先注册一个节点"
        registry.register("CREATE_ORDER", "创建订单")

        when: "用新的name重新注册"
        registry.register("CREATE_ORDER", "下单(V2)")

        then: "name被更新"
        registry.getNode("CREATE_ORDER").name == "下单(V2)"

        and: "不会出现重复节点"
        registry.getAllNodes().size() == 1
    }

    // ==================== Thread Safety ====================

    def "并发注册不丢失数据"() {
        given: "准备100个不同code的注册任务"
        def threads = (1..100).collect { i ->
            new Thread({ registry.register("NODE_${i}", "节点${i}") })
        }

        when: "并发执行"
        threads.each { it.start() }
        threads.each { it.join() }

        then: "100个节点全部注册成功"
        registry.getAllNodes().size() == 100
    }
}
