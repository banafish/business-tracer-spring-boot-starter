# business-tracer-spring-boot-starter

业务链路追踪 Spring Boot Starter：通过 `@BusinessTrace` 注解 + DSL 流程定义记录业务节点日志，
内置告警中心（规则/渠道/事件/派发）。产物是 Java 8 字节码的库，供业务方引入。

## 构建与验证

构建需要 JDK 17+（本机 `JAVA_HOME` 已指向 JDK 21）；产物是 Java 8 字节码（`maven.compiler.release=8`），
**不得使用 Java 8 之后的 API**。

| 目的 | 命令 |
| --- | --- |
| 全量验证（提交前必须绿） | `mvn -B verify` |
| 跑单个 Spec | `mvn -B test "-Dtest=FlowLogServiceSpec"` |
| 格式化代码 | `mvn -B spotless:apply` |
| 覆盖率报告 | `target/site/jacoco/index.html`（verify 后生成） |

`mvn verify` 包含的门禁：Spotless 格式检查、SpotBugs（Max/Medium）、JaCoCo 行覆盖率阈值（只升不降）、
Maven Enforcer、ArchUnit 架构测试。任何一项失败都会阻断构建，CI（GitHub Actions）跑同一条命令。

## 架构（DDD 分层）

`com.bananice.businesstracer` 下：

- `api/` — 对外公开的注解与门面（`@BusinessTrace`、`BusinessTracer`）
- `domain/` — 领域模型与仓储接口，**不依赖其他任何层**
- `application/` — 应用服务，只依赖 domain
- `infrastructure/` — MyBatis-Plus 持久化、AOP 切面、告警渠道、定时任务
- `presentation/` — REST 控制器
- `config/` — Spring 自动装配入口（`BusinessTracerAutoConfiguration`）

分层规则由 `ArchitectureTest` 强制；存量违规冻结在 `src/test/resources/archunit-store`，
**只拦新增违规**。消除一条存量违规后重跑测试并提交收缩后的 store。

## 约定

- 测试用 Spock（Groovy），命名 `XxxSpec.groovy`；测试数据用 `fixtures/DomainFixtures`
  （defaults + overrides 模式），不要在新 Spec 里手写私有 build helper。
- 持久化测试基于 H2 + `schema.sql`，不引入 Docker/Testcontainers。
- 提交信息用 conventional commits（`feat:` `fix:` `build:` `test:` `docs:` `chore:` `ci:`）。
- 豁免 SpotBugs（`spotbugs-exclude.xml` / `@SuppressFBWarnings`）或冻结 ArchUnit 违规，
  **必须写明原因**。
- 覆盖率阈值在 `pom.xml` jacoco `check-coverage` 处，每提升约 5% 手动上调，调整独立成 commit。
