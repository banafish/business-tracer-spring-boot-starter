package com.bananice.businesstracer.infrastructure.aspect

import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.lang.reflect.Method

class SpelParserSpec extends Specification {

    @Subject
    SpelParser spelParser = new SpelParser()

    // ==================== Helper ====================

    /**
     * 用于提供带参数名的Method对象（需要编译时 -parameters 或依赖Spring的ParameterNameDiscoverer）
     * 这里直接定义样例方法并反射获取
     */
    static String sampleMethod(String orderId, Integer amount) { return null }
    static String multiArgMethod(String name, Integer age, List<String> tags) { return null }
    static String noArgMethod() { return null }

    private Method getMethod(String name, Class<?>... paramTypes) {
        return SpelParserSpec.getDeclaredMethod(name, paramTypes)
    }

    // ==================== Basic Parsing ====================

    def "解析简单的SpEL表达式引用方法参数"() {
        given: "sampleMethod(orderId, amount) 的Method对象"
        def method = getMethod("sampleMethod", String, Integer)
        def args = ["ORD-001", 100] as Object[]

        when: "解析 #orderId 表达式"
        def result = spelParser.parse("#orderId", method, args)

        then: "返回参数值"
        result == "ORD-001"
    }

    def "解析引用Integer参数的SpEL表达式"() {
        given:
        def method = getMethod("sampleMethod", String, Integer)
        def args = ["ORD-001", 250] as Object[]

        when:
        def result = spelParser.parse("#amount", method, args)

        then: "Integer转为字符串"
        result == "250"
    }

    // ==================== Null / Empty ====================

    @Unroll
    def "SpEL表达式为 #desc 时返回null"() {
        given:
        def method = getMethod("sampleMethod", String, Integer)
        def args = ["ORD-001", 100] as Object[]

        expect:
        spelParser.parse(spel, method, args) == null

        where:
        desc   | spel
        "null" | null
        "空字符串" | ""
    }

    // ==================== With Result (4-arg parse) ====================

    def "带result参数的parse方法可以引用#result变量"() {
        given:
        def method = getMethod("sampleMethod", String, Integer)
        def args = ["ORD-001", 100] as Object[]
        def result = "SUCCESS"

        when: "解析引用 #result 的表达式"
        def parsed = spelParser.parse("#result", method, args, result)

        then:
        parsed == "SUCCESS"
    }

    def "带result参数时仍然可以引用方法参数"() {
        given:
        def method = getMethod("sampleMethod", String, Integer)
        def args = ["ORD-001", 100] as Object[]

        when:
        def parsed = spelParser.parse("#orderId", method, args, "any-result")

        then:
        parsed == "ORD-001"
    }

    def "result为null时 #result 解析为null"() {
        given:
        def method = getMethod("sampleMethod", String, Integer)
        def args = ["ORD-001", 100] as Object[]

        when:
        def parsed = spelParser.parse("#result", method, args, null)

        then:
        parsed == null
    }

    // ==================== Complex Expressions ====================

    def "支持SpEL字符串拼接表达式"() {
        given:
        def method = getMethod("sampleMethod", String, Integer)
        def args = ["ORD-001", 100] as Object[]

        when:
        def result = spelParser.parse("#orderId + '-' + #amount", method, args)

        then:
        result == "ORD-001-100"
    }

    def "支持SpEL三目运算符"() {
        given:
        def method = getMethod("sampleMethod", String, Integer)
        def args = ["ORD-001", 100] as Object[]

        when:
        def result = spelParser.parse("#amount > 50 ? 'HIGH' : 'LOW'", method, args)

        then:
        result == "HIGH"
    }

    // ==================== Collection / Map ====================

    def "解析集合类型参数时自动JSON序列化"() {
        given:
        def method = getMethod("multiArgMethod", String, Integer, List)
        def args = ["Alice", 30, ["vip", "test"]] as Object[]

        when: "解析List类型参数"
        def result = spelParser.parse("#tags", method, args)

        then: "返回JSON数组字符串"
        result == '["vip","test"]'
    }

    // ==================== Expression Cache ====================

    def "多次解析相同表达式使用缓存不报错"() {
        given:
        def method = getMethod("sampleMethod", String, Integer)
        def args1 = ["ORD-001", 100] as Object[]
        def args2 = ["ORD-002", 200] as Object[]

        when: "连续两次解析相同表达式但不同参数"
        def result1 = spelParser.parse("#orderId", method, args1)
        def result2 = spelParser.parse("#orderId", method, args2)

        then: "每次返回正确的值"
        result1 == "ORD-001"
        result2 == "ORD-002"
    }

    // ==================== No-arg Method ====================

    def "无参方法也能正常解析常量表达式"() {
        given:
        def method = getMethod("noArgMethod")
        def args = [] as Object[]

        when:
        def result = spelParser.parse("'constant-value'", method, args)

        then:
        result == "constant-value"
    }
}
