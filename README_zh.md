<div align="center">

<img src="https://img.shields.io/badge/Java-8+-blue" alt="Java 8+"> <img src="https://img.shields.io/badge/Spring%20Boot-2.x-brightgreen" alt="Spring Boot 2.x"> <img src="https://img.shields.io/badge/MySQL-8+-blue" alt="MySQL 8+">

<h3>Business Tracer Spring Boot Starter</h3>

<a href="README.md">English</a> | 中文

</div>

---

Business Tracer 是一个基于 Spring Boot 的轻量级分布式业务链路追踪组件。它允许开发者在跨多个服务及分散的技术链路中，使用统一的 **业务ID (Business ID)** 来追踪一个业务对象（例如订单）的完整生命周期。

与传统的 APM 工具（基于 TraceId）不同，Business Tracer 根据**业务身份**来聚合日志，从而解决了在长时间运行的业务流程中（如：创建订单 -> 支付 -> 发货）日志碎片化的问题。

## 功能特性 (Features)

- **以业务ID为中心 (Business ID Centric)**: 跨多个请求和微服务，根据业务ID（如订单号）聚合零散的日志。
- **节点与详情追踪 (Node & Detail Tracking)**:
    - **节点 (Node)**: 在方法上使用 `@BusinessTrace` 注解定义的高层级步骤。它能通过 SpEL 追踪执行耗时 (`cost_time`)、输入/输出参数，以及异常信息。
    - **详情 (Detail)**: 通过在节点内部调用 `BusinessTracer.record()` 和 `BusinessTracer.recordError()` 记录的细粒度执行步骤或错误信息。
- **可视化追踪 UI 与 DSL 编辑器 (Visual Trace UI & DSL Editor)**: 内置基于 Drawflow 的可视化界面，用于设计业务流程（DSL）并提供实时的执行链路视图。节点中展示了耗时、参数、异常等，并使用颜色对状态进行编码配置（如：绿色代表成功，红色代表失败）。
- **高级错误处理 (Advanced Error Handling)**: 自动捕获方法的各级异常。同时也支持在代码中调用 `BusinessTracer.recordError()`，以编程式的方式主动触发节点或流程失败。
- **分布式上下文传递 (Distributed Propagation)**: 自动通过 HTTP 请求头（`X-Business-Id`）在微服务之间进行业务ID与链路上下文的传播。
- **MDC 集成 (MDC Integration)**: 自动将 `businessId` 和 `traceId` 注入到 SLF4J 的 MDC 诊断上下文中，便于与标准应用日志相关联。
- **领域驱动设计 (Domain-Driven Design, DDD)**: 采用 DDD 原则构建项目架构，使其具有明确的边界，更易于维护和测试。项目中对 Spock + H2 的单元测试具有极好的支持。

## 快速开始 (Quick Start)

### 1. 环境要求 (Requirements)

- Java 8+
- Spring Boot 2.x
- MySQL 数据库

### 2. 引入依赖 (Dependency)

将以下依赖添加到你的项目 `pom.xml` 中:

```xml
<dependency>
    <groupId>com.bananice</groupId>
    <artifactId>business-tracer-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

如果是本地开发阶段，你可能需要先将其安装到本地的 Maven 仓库：

```bash
mvn clean install
```

### 3. 初始化数据库 (Database Setup)

请执行初始化脚本 (`sql/init.sql`) 创建必要的数据库表：
- `business_flow_dsl`: 存储流程定义的 DSL 配置 (JSON 格式)。
- `business_flow_log`: 顶层的流程追踪记录日志。
- `business_trace_node`: 追踪流程中的每一个过程步骤（即节点），包含参数、执行耗时、异常及状态信息。
- `business_trace_detail`: 追踪位于节点内部生成的详细日志与错误记录。
- `business_alert_rule`: 告警规则表，按 `GLOBAL` / `FLOW` / `NODE` 作用域存储规则。
- `business_alert_channel`: 告警通道表，存储 WEBHOOK/WECOM/DINGTALK/EMAIL 通道。
- `business_alert_event`: 告警事件表，记录运行时评估产出的告警事件。
- `business_alert_dispatch_log`: 告警投递日志表，记录事件在各通道的投递尝试。
- `business_alert_config_version`: 告警配置版本表，用于多实例配置缓存同步。

### 4. 配置文件 (Configuration)

然后在 `application.yml` 或 `application.properties` 文件中配置你的数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/your_db?useSSL=false
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
  application:
    name: your-service-name

business-tracer:
  alert:
    # 运行时阈值
    slow-node-threshold-ms: 2000
    flow-stuck-threshold-ms: 300000

    # 调度开关与周期
    scheduling-enabled: true
    config-sync-fixed-delay-ms: 5000
    flow-stuck-scan-fixed-delay-ms: 60000
    aggregation-flush-fixed-delay-ms: 60000
    history-cleanup-fixed-delay-ms: 3600000

    # 留存与投递策略
    retention-days: 30
    dispatch-attempt-timeout-ms: 1000
    dispatch-max-retries: 1
```

## 使用指南 (Usage)

### 1. 注解模式 (定义节点)

使用 `@BusinessTrace` 标注在你的 Service 层方法上，以在业务流中定义一个“节点（Node）”。 你可以使用 SpEL 表达式来动态提取 Business ID，以及获取出入参数的信息。

```java
import com.bananice.businesstracer.api.BusinessTrace;

@Service
public class OrderService {

    @BusinessTrace(
        code = "CREATE_ORDER", // 节点的唯一标识符，这在 DSL 流程编排中十分关键
        name = "Create Order", // 可以读懂的具有业务含义的中文名词
        key = "#order.orderId", // 用于提取业务ID的 SpEL 表达式
        operation = "Create a new order", // 方法相关的操作描述
        inputParams = "#order",
        outputParams = "#result"
    )
    public OrderResult createOrder(Order order) {
        // 你的核心业务逻辑...
        return new OrderResult(true);
    }
}
```
如果在带有 `@BusinessTrace` 的方法内发生未捕获的异常，该节点及其所属的顶级父流程会被自动标记为 `FAILED`（失败），同时会自动记录堆栈跟踪。该节点的执行时间 (`cost_time`) 也是自动统计的。

### 2. 编程式模式 (记录详情日志与错误)

在经过追踪的方法（节点）内部，你可以使用 `BusinessTracer` 追加详尽的步骤日志，也可以显式记录引发失败的关键错误，此时无需实际去抛出一个异常就能标记节点为 failed。

```java
import com.bananice.businesstracer.api.BusinessTracer;

@Service
public class OrderService {

    @BusinessTrace(code = "PAY", key = "#orderId", name = "Process Payment")
    public void pay(String orderId) {
        BusinessTracer.record("Payment validation successful"); // 添加一条正常的详情日志
        
        if (paymentFails()) {
            // 这会录入一条状态为 FAILED 的日志，同时导致外层此节点与全局整个流程标记为 FAILED 异常状态
            BusinessTracer.recordError("Gateway timeout");
        }
    }
}
```

### 3. 可视化追踪界面 (Visual Trace UI)

组件内置提供了一套直观的 Web 界面，可以对业务的流转生命周期与追踪路线做到可视化管理。一旦你的应用程序成功启动，可以通过以下路径访问 UI 控制台：

**`http://localhost:8080/business-tracer/index.html`**

![DSL Management](img/image1.png)

- **DSL 管理 (DSL Management)**: 利用可视化图形编辑器创建和编辑具体的业务流配置。

![Flow Logs](img/image3.png)

- **链路可视化展示 (Trace Visualization)**: 可以查看任意一个指定 `businessId` 的执行演进路线，这其中包括了节点的状态（成功/失败）、节点间耗费的时间，以及详尽具体的内层日志。

![Trace Visualization](img/image2.png)

- **API 接口 (API Endpoints)**: 该 UI 内部是通过拉取类似于 `/business-tracer/api/flow-logs` 和 `/business-tracer/trace?businessId=...` 等端点接口来进行数据的通讯交互的。

### 4. 告警中心（规则 / 通道 / 静默 / 历史）

Business Tracer 内置了告警中心页面，用于运维侧统一管理规则、通道和历史告警。

访问地址：

**`http://localhost:8080/business-tracer/alerts.html`**

核心页签：
- **规则（Rules）**：按 `NODE(flow+node) > FLOW(flow) > GLOBAL` 优先级配置规则。
- **通道（Channels）**：维护告警通道并执行 test-send 测试发送。
- **静默（Silence）**：维护静默窗口（当前实现保存在浏览器 localStorage）。
- **历史（History）**：按类型/状态/时间筛选告警事件，并查看 dispatch logs。

告警中心使用的主要 API：
- `GET /business-tracer/api/alerts/rules`
- `PUT /business-tracer/api/alerts/rules/{scopeType}/{scopeCode}`
- `GET /business-tracer/api/alerts/channels`
- `POST /business-tracer/api/alerts/channels`
- `PUT /business-tracer/api/alerts/channels/{id}`
- `POST /business-tracer/api/alerts/channels/{id}/test-send`
- `GET /business-tracer/api/alerts/events`
- `GET /business-tracer/api/alerts/events/{id}/dispatch-logs`

### 5. 分布式追踪 (Distributed Tracing)

如果在代码里通过 HTTP 调用下级的系统服务，本组件会自动使用标准 Header 机制来继续传递追踪上下文。

向下游传播的 Header 头部如下：
- `X-Business-Id`
- `X-Trace-Id`
- `X-Parent-Node-Id`

你需要确保下游所有涉及处理的微服务同样引入并启用了本 Starter，这样才能使所有跨库与跨服务系统的日志串联合并在一起。

### 5. 异步上下文传递 (Asynchronous Context Propagation)

Business Tracer 提供了内置的支持，用于跨异步边界（例如：子线程、`@Async` 方法或线程池）传递追踪上下文。

**1. 普通的子线程:**
由于内部的上下文持有器使用了 `InheritableThreadLocal`，在有活跃追踪上下文的业务代码中，直接通过 `new Thread()` 启动的任何普通子线程，会自动继承 `TraceContext`。

**2. 线程池与 `@Async`:**
当使用线程池时，因为线程会被池化和复用，会导致默认的继承机制失效。为了确保业务上下文和 MDC 能顺利传递给线程池内部运行的任务（也包含标准的 Spring `@Async` 方法），你需要将项目中的 `ThreadPoolTaskExecutor` 显式配置挂载 `TraceContextTaskDecorator` 装饰器：

```java
import com.bananice.businesstracer.infrastructure.context.TraceContextTaskDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ExecutorConfig {

    @Bean("myTaskExecutor")
    public ThreadPoolTaskExecutor myTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        // ... 其他配置项 ...
        // 关键步骤：添加 TaskDecorator 来接力传递 TraceContext
        executor.setTaskDecorator(new TraceContextTaskDecorator());
        executor.initialize();
        return executor;
    }
}
```

配置完成后，任何提交给 `myTaskExecutor` 运行的异步方法都会无缝继承父线程的“业务ID (Business ID)”。这意味着你可以在所有的异步任务内部放心使用 `BusinessTracer.record()`、`BusinessTracer.recordError()`，而标准 SLF4J 的 MDC 输出也能正常工作。

## 架构说明 (Architecture)

整体的项目代码结构严格遵循了 **领域驱动设计 (DDD)** 的思想与原则：

- **API层**: 提供外部使用的主力公共注解 (`@BusinessTrace`) 及其静态辅助工具 (`BusinessTracer`)。
- **Application层**: 定义了处理领域逻辑的应用服务 (`DslService`, `FlowLogService` 等)。
- **Domain层**: 保护所有核心业务实体模型 (`NodeLog`, `DetailLog`, `FlowLog`, `DslConfig`) 并声名了相关的资源库接口（Repository）。
- **Infrastructure层**: 提供基础持久化支持、MVC 视图控制器 (Presentation)、上下文持有机制 (ThreadLocal / MDC) 以及将这些拦截逻辑揉进 Spring Boot 的自动配置实现中去。
- **Tests测试层**: 利用 **Spock 框架** 并联合底层的 H2 内存数据库实现了详尽完善的覆盖单元测试。

## 许可证 (License)

MIT
