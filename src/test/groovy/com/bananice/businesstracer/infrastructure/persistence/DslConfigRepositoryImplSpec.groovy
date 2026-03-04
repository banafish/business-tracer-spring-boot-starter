package com.bananice.businesstracer.infrastructure.persistence

import com.bananice.businesstracer.TestApplication
import com.bananice.businesstracer.domain.model.DslConfig
import com.bananice.businesstracer.domain.repository.DslConfigRepository
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

import javax.annotation.Resource

@SpringBootTest(classes = TestApplication)
@Transactional
class DslConfigRepositoryImplSpec extends Specification {

    @Resource
    @Subject
    DslConfigRepository dslConfigRepository

    @Shared
    def counter = 0

    private DslConfig buildDslConfig(Map overrides = [:]) {
        counter++
        def defaults = [flowCode: "flow-${counter}", name: "Test Flow ${counter}", layout: "timeline"]
        def merged = defaults + overrides
        def dsl = new DslConfig()
        dsl.flowCode = merged.flowCode as String
        dsl.name = merged.name as String
        dsl.layout = merged.layout as String
        dsl
    }

    def "保存DslConfig后能通过flowCode查到"() {
        given:
        def dsl = buildDslConfig(flowCode: "flow-save", name: "保存测试")

        when:
        dslConfigRepository.save(dsl)
        def found = dslConfigRepository.findByFlowCode("flow-save")

        then:
        found != null
        found.name == "保存测试"
    }

    def "findByFlowCode查找不存在的返回null"() {
        expect:
        dslConfigRepository.findByFlowCode("non-existent") == null
    }

    def "existsByFlowCode正确判断"() {
        given:
        dslConfigRepository.save(buildDslConfig(flowCode: "flow-ex"))

        expect:
        dslConfigRepository.existsByFlowCode("flow-ex")
        !dslConfigRepository.existsByFlowCode("flow-no")
    }

    def "deleteByFlowCode成功删除返回true"() {
        given:
        dslConfigRepository.save(buildDslConfig(flowCode: "flow-del"))

        expect:
        dslConfigRepository.deleteByFlowCode("flow-del")
        dslConfigRepository.findByFlowCode("flow-del") == null
    }

    def "deleteByFlowCode不存在时返回false"() {
        expect:
        !dslConfigRepository.deleteByFlowCode("no-such")
    }

    def "支持不同layout类型"() {
        given:
        ["timeline", "tree", "flow"].each { layout ->
            dslConfigRepository.save(buildDslConfig(flowCode: "flow-l-${layout}", layout: layout))
        }

        expect:
        dslConfigRepository.findByFlowCode("flow-l-timeline").layout == "timeline"
        dslConfigRepository.findByFlowCode("flow-l-tree").layout == "tree"
        dslConfigRepository.findByFlowCode("flow-l-flow").layout == "flow"
    }
}
