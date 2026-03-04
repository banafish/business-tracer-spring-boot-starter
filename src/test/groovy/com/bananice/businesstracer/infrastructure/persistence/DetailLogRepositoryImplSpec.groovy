package com.bananice.businesstracer.infrastructure.persistence

import com.bananice.businesstracer.TestApplication
import com.bananice.businesstracer.domain.model.DetailLog
import com.bananice.businesstracer.domain.repository.DetailLogRepository
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
class DetailLogRepositoryImplSpec extends Specification {

    @Resource
    @Subject
    DetailLogRepository detailLogRepository

    @Shared
    def now = LocalDateTime.now()

    // ==================== Helper Methods ====================

    private DetailLog buildDetailLog(Map overrides = [:]) {
        def defaults = [
                businessId  : "business-default",
                parentNodeId: "parentNode-default",
                content     : "Default Content",
                status      : "NORMAL",
                createTime  : now
        ]
        def merged = defaults + overrides
        DetailLog.builder()
                .businessId(merged.businessId as String)
                .parentNodeId(merged.parentNodeId as String)
                .content(merged.content as String)
                .status(merged.status as String)
                .createTime(merged.createTime as LocalDateTime)
                .build()
    }

    // ==================== Save & Query ====================

    def "保存DetailLog后能通过parentNodeId查询到"() {
        given: "构建一条DetailLog记录"
        def log = buildDetailLog(
                businessId: "business-123",
                parentNodeId: "parentNode-123",
                content: "Test Content"
        )

        when: "保存并查询"
        detailLogRepository.save(log)
        def results = detailLogRepository.findByParentNodeId("parentNode-123")

        then: "查询结果与保存数据一致"
        results.size() == 1
        verifyAll(results[0]) {
            businessId == "business-123"
            parentNodeId == "parentNode-123"
            content == "Test Content"
            status == "NORMAL"
        }
    }

    def "查询不存在的parentNodeId应返回空列表"() {
        expect: "不存在的parentNodeId应返回空集合"
        detailLogRepository.findByParentNodeId("no-such-parent").isEmpty()
    }

    // ==================== Multiple Records ====================

    def "同一parentNodeId下保存多条记录能全部查出"() {
        given: "在同一parentNodeId下保存3条DetailLog"
        def parentId = "parent-multi"
        (1..3).each { i ->
            detailLogRepository.save(
                    buildDetailLog(parentNodeId: parentId, content: "Content-${i}")
            )
        }

        when: "按parentNodeId查询"
        def results = detailLogRepository.findByParentNodeId(parentId)

        then: "返回全部3条记录"
        results.size() == 3

        and: "每条记录内容各不相同"
        results*.content.toSet().size() == 3
    }

    def "不同parentNodeId的记录互不干扰"() {
        given: "分别保存属于不同parentNode的记录"
        detailLogRepository.save(buildDetailLog(parentNodeId: "parent-A", content: "A"))
        detailLogRepository.save(buildDetailLog(parentNodeId: "parent-B", content: "B"))

        expect: "各自只能查到属于自己的记录"
        detailLogRepository.findByParentNodeId("parent-A").size() == 1
        detailLogRepository.findByParentNodeId("parent-B").size() == 1
        detailLogRepository.findByParentNodeId("parent-A")[0].content == "A"
        detailLogRepository.findByParentNodeId("parent-B")[0].content == "B"
    }

    // ==================== Data Table Parameterized Tests ====================

    @Unroll
    def "保存状态为 #status 的DetailLog能正确持久化"() {
        given: "构建指定状态的DetailLog"
        def log = buildDetailLog(
                parentNodeId: "parent-status-${status}",
                status: status,
                content: content
        )

        when: "保存并查询"
        detailLogRepository.save(log)
        def result = detailLogRepository.findByParentNodeId("parent-status-${status}")

        then: "状态和内容正确持久化"
        verifyAll(result[0]) {
            it.status == status
            it.content == content
        }

        where: "不同的状态场景"
        status   | content
        "NORMAL" | "正常日志内容"
        "FAILED" | "异常时的详细日志"
    }

    @Unroll
    def "DetailLog的content支持 #desc"() {
        given: "构建特定内容的DetailLog"
        def log = buildDetailLog(parentNodeId: "parent-content-${index}", content: contentValue)

        when: "保存并查询"
        detailLogRepository.save(log)
        def result = detailLogRepository.findByParentNodeId("parent-content-${index}")

        then: "内容正确持久化"
        result[0].content == contentValue

        where: "不同的内容类型"
        desc       | contentValue                                  | index
        "短文本"     | "ok"                                          | 1
        "JSON字符串" | '{"key":"value","list":[1,2,3]}'              | 2
        "多行文本"    | "第一行\n第二行\n第三行"                             | 3
        "空字符串"    | ""                                             | 4
    }

    // ==================== Edge Cases ====================

    def "DetailLog保存后自动生成id"() {
        given: "构建一条未设置id的DetailLog"
        def log = buildDetailLog(parentNodeId: "parent-id-gen")

        when: "保存并查询"
        detailLogRepository.save(log)
        def results = detailLogRepository.findByParentNodeId("parent-id-gen")

        then: "查到记录且id已自动生成"
        results.size() == 1
        results[0].id != null
    }
}
