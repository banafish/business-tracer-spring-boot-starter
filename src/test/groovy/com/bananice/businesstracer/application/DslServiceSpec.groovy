package com.bananice.businesstracer.application

import com.bananice.businesstracer.application.dto.DslRenderNode
import com.bananice.businesstracer.domain.model.DslConfig
import com.bananice.businesstracer.domain.model.DslNode
import com.bananice.businesstracer.domain.model.NodeLog
import com.bananice.businesstracer.domain.repository.DslConfigRepository
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.time.LocalDateTime

class DslServiceSpec extends Specification {

    DslConfigRepository dslConfigRepository = Mock()

    @Subject
    DslService dslService = new DslService(dslConfigRepository)

    // ==================== Helper ====================

    private DslConfig buildDslConfig(String flowCode, String name, List<DslNode> nodes = []) {
        def dsl = new DslConfig()
        dsl.flowCode = flowCode
        dsl.name = name
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

    private NodeLog buildNodeLog(String code, String businessId = "bus-1") {
        NodeLog.builder()
                .businessId(businessId)
                .code(code)
                .name("Node $code")
                .nodeId(UUID.randomUUID().toString())
                .traceId("trace-1")
                .status("COMPLETED")
                .costTime(100L)
                .createTime(LocalDateTime.now())
                .build()
    }

    // ==================== getAllDsl ====================

    def "getAllDsl委托给repository的findAll"() {
        given:
        def dsls = [buildDslConfig("flow-1", "Flow1"), buildDslConfig("flow-2", "Flow2")]
        dslConfigRepository.findAll() >> dsls

        when:
        def result = dslService.getAllDsl()

        then:
        result.size() == 2
        result*.flowCode == ["flow-1", "flow-2"]
    }

    // ==================== getDslByFlowCode ====================

    def "getDslByFlowCode传入null返回null"() {
        expect:
        dslService.getDslByFlowCode(null) == null
    }

    def "getDslByFlowCode正常查询"() {
        given:
        def dsl = buildDslConfig("flow-abc", "ABC流程")
        dslConfigRepository.findByFlowCode("flow-abc") >> dsl

        expect:
        dslService.getDslByFlowCode("flow-abc") == dsl
    }

    // ==================== createDsl ====================

    def "createDsl在flowCode不存在时正常保存"() {
        given:
        def dsl = buildDslConfig("new-flow", "新流程")
        dslConfigRepository.existsByFlowCode("new-flow") >> false
        dslConfigRepository.save(dsl) >> dsl

        when:
        def result = dslService.createDsl(dsl)

        then:
        result.flowCode == "new-flow"
    }

    def "createDsl在flowCode已存在时抛IllegalArgumentException"() {
        given:
        def dsl = buildDslConfig("dup-flow", "重复流程")
        dslConfigRepository.existsByFlowCode("dup-flow") >> true

        when:
        dslService.createDsl(dsl)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("dup-flow")
    }

    // ==================== updateDsl ====================

    def "updateDsl在flowCode存在时正常更新"() {
        given:
        def dsl = buildDslConfig("upd-flow", "更新流程")
        dslConfigRepository.existsByFlowCode("upd-flow") >> true
        dslConfigRepository.update(_ as DslConfig) >> { args -> args[0] }

        when:
        def result = dslService.updateDsl("upd-flow", dsl)

        then:
        result.flowCode == "upd-flow"
    }

    def "updateDsl在flowCode不存在时抛IllegalArgumentException"() {
        given:
        dslConfigRepository.existsByFlowCode("missing") >> false

        when:
        dslService.updateDsl("missing", new DslConfig())

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("missing")
    }

    // ==================== deleteDsl ====================

    def "deleteDsl委托给repository的deleteByFlowCode"() {
        given:
        dslConfigRepository.deleteByFlowCode("del-flow") >> true

        expect:
        dslService.deleteDsl("del-flow")
    }

    // ==================== findDslsByNodeCode ====================

    def "findDslsByNodeCode传入null返回空列表"() {
        expect:
        dslService.findDslsByNodeCode(null).isEmpty()
    }

    def "findDslsByNodeCode能找到包含指定nodeCode的DSL"() {
        given: "两个DSL配置，其中一个包含目标nodeCode"
        def nodeA = buildDslNode("CREATE_ORDER")
        def nodeB = buildDslNode("PAYMENT")
        def dsl1 = buildDslConfig("flow-1", "流程1", [nodeA, nodeB])
        def dsl2 = buildDslConfig("flow-2", "流程2", [buildDslNode("SHIPPING")])
        dslConfigRepository.findAll() >> [dsl1, dsl2]

        when:
        def result = dslService.findDslsByNodeCode("PAYMENT")

        then: "只返回包含PAYMENT的DSL"
        result.size() == 1
        result[0].flowCode == "flow-1"
    }

    def "findDslsByNodeCode能搜索嵌套子节点"() {
        given: "DSL有嵌套子节点"
        def childNode = buildDslNode("DEEP_NODE")
        def parentNode = buildDslNode("PARENT", false, [childNode])
        def dsl = buildDslConfig("flow-deep", "深层流程", [parentNode])
        dslConfigRepository.findAll() >> [dsl]

        when:
        def result = dslService.findDslsByNodeCode("DEEP_NODE")

        then:
        result.size() == 1
        result[0].flowCode == "flow-deep"
    }

    // ==================== renderByDsl ====================

    def "renderByDsl在dsl为null时以平铺列表返回日志"() {
        given:
        def logs = [buildNodeLog("CODE_A"), buildNodeLog("CODE_B")]

        when:
        def result = dslService.renderByDsl(logs, null)

        then:
        result.flowCode == null
        result.dslName == null
        result.layout == "timeline"
        result.nodes.size() == 2
    }

    def "renderByDsl按DSL结构组织日志"() {
        given: "DSL定义了两个节点"
        def dsl = buildDslConfig("flow-render", "渲染流程", [
                buildDslNode("NODE_A"),
                buildDslNode("NODE_B")
        ])

        and: "对应的日志"
        def logs = [buildNodeLog("NODE_A"), buildNodeLog("NODE_B")]

        when:
        def result = dslService.renderByDsl(logs, dsl)

        then:
        result.flowCode == "flow-render"
        result.nodes.size() == 2
        result.nodes[0].code == "NODE_A"
        result.nodes[1].code == "NODE_B"
    }

    def "renderByDsl将不在DSL中的日志放入orphans"() {
        given: "DSL只定义了NODE_A"
        def dsl = buildDslConfig("flow-orphan", "孤儿测试", [buildDslNode("NODE_A")])

        and: "但日志中有NODE_A和UNKNOWN_NODE"
        def logs = [buildNodeLog("NODE_A"), buildNodeLog("UNKNOWN_NODE")]

        when:
        def result = dslService.renderByDsl(logs, dsl)

        then: "UNKNOWN_NODE进入orphans"
        result.nodes.size() == 1
        result.orphans.size() == 1
        result.orphans[0].code == "UNKNOWN_NODE"
    }

    @Unroll
    def "renderByDsl处理DSL节点无日志匹配时 (#desc)"() {
        given:
        def dsl = buildDslConfig("flow-empty", "空日志", [buildDslNode("NODE_NO_LOG")])

        when:
        def result = dslService.renderByDsl(logs, dsl)

        then: "nodes中有节点但logs为空"
        def node = result.nodes[0]
        node.code == "NODE_NO_LOG"
        node.logs.isEmpty()

        where:
        desc     | logs
        "空日志列表" | []
        "无匹配的日志" | [buildNodeLog("OTHER_CODE")]
    }
}
