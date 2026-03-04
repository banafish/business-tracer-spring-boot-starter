package com.bananice.businesstracer.fixture;

import com.bananice.businesstracer.api.BusinessTrace;
import com.bananice.businesstracer.api.BusinessTracer;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 专门用于集成测试 BusinessTraceAspect 的测试服务。
 * 每个方法模拟一种典型的 @BusinessTrace 使用场景。
 */
@Service
public class TracedTestService {

    @Lazy
    @Resource
    private TracedTestService self;

    /**
     * 基础场景：只有 code 和 key
     */
    @BusinessTrace(code = "BASIC_NODE", key = "#orderId")
    public String basicMethod(String orderId) {
        return "ok-" + orderId;
    }

    /**
     * 完整场景：含 name、operation、inputParams、outputParams
     */
    @BusinessTrace(code = "FULL_NODE", name = "完整节点", key = "#bizId", operation = "执行完整操作", inputParams = "#bizId", outputParams = "#result")
    public String fullMethod(String bizId) {
        return "result-" + bizId;
    }

    /**
     * 异常场景：方法内部抛出异常
     */
    @BusinessTrace(code = "ERROR_NODE", key = "#id")
    public void throwingMethod(String id) {
        throw new RuntimeException("测试异常: " + id);
    }

    /**
     * 手动标记错误场景：调用 BusinessTracer.recordError()
     */
    @BusinessTrace(code = "MANUAL_ERROR_NODE", key = "#id")
    public String manualErrorMethod(String id) {
        BusinessTracer.recordError("手动记录的错误: " + id);
        return "done";
    }

    /**
     * 嵌套调用场景（外层）：通过self代理调用内层，确保AOP生效
     */
    @BusinessTrace(code = "OUTER_NODE", key = "#id")
    public String outerMethod(String id) {
        return self.innerMethod(id);
    }

    /**
     * 嵌套调用场景（内层）
     */
    @BusinessTrace(code = "INNER_NODE", key = "#id")
    public String innerMethod(String id) {
        return "inner-" + id;
    }
}
