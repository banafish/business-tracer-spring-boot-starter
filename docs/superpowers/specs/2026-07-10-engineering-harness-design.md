# 工程 Harness 落地设计（business-tracer-spring-boot-starter）

日期：2026-07-10
状态：已获用户批准（方案 A：精选工具、一次到位）

## 背景与现状

项目是单模块 Maven 工程（Spring Boot 2.7.12 父 POM，目标 Java 8 字节码），约 94 个
Java 源文件，DDD 分层（api / application / config / domain / infrastructure /
presentation）。测试为 Spock 2.2 + Groovy 3.0.17 + H2，约 28 个测试类，由
gmavenplus-plugin 编译、surefire 匹配 `**/*Spec.java` 与 `**/*Test.java`。

现状缺口：无 CI 流水线、无格式化与静态检查、无覆盖率门禁、无架构守护测试、无
CLAUDE.md 与 `.claude/` 协作配置。

## 目标

1. 一条命令（`mvn verify`）跑通全部质量门禁，本地与 CI 行为一致。
2. 存量代码一次性清理到干净基线，之后所有门禁严格阻断。
3. 测试基础设施升级（架构守护 + 测试数据构造），不引入 Docker。
4. AI 编码智能体（Claude Code 等）在本项目内可高效、低打断地工作。

## 非目标

- 不做发布/部署流水线（starter 库的发版流程另行处理）。
- 不引入 Testcontainers 或任何 Docker 依赖，测试继续基于 H2。
- 不强制回改全部 28 个既有 Spec 去使用新的测试数据 Builder。
- 不引入 Checkstyle / PMD / Error Prone / PIT 等与已选工具职责重叠的工具。

## 约束

- 产物必须是 Java 8 字节码（对使用方的兼容承诺不变）。
- 构建环境使用 JDK 17+ 跑 Maven（本地已具备；CI 同样用 17），以解锁新版工具链。
- 用户本地与 CI 均不使用 Docker。

## 第 1 节 · 工程质量门禁（pom.xml 改造）

所有门禁绑定 `mvn verify` 生命周期，一条命令全量检查。

### 编译基线

- 将 `java.version=1.8`（source/target）改为 `maven.compiler.release=8`：
  用 JDK 17 构建，严格产出 Java 8 字节码并校验 Java 8 API 边界，
  可拦截误用高版本 JDK API。

### Spotless（格式化）

- 引擎：palantir-java-format；同时管理 import 顺序、行尾、去尾随空白、POM 排序。
- 作用范围：Java 源码（main + test 的 `*.java`）。Groovy Spec 暂不纳入格式化范围
  （palantir 不支持 Groovy，greclipse 收益低），后续需要时另行评估。
- 首次执行 `mvn spotless:apply` 全量格式化全部 Java 文件，独立成一个 commit
  （不与其他改动混合，保证 blame 可切割）。
- 之后 `spotless:check` 绑定 verify 阶段，格式不符即构建失败。

### SpotBugs（静态缺陷检查）

- `spotbugs:check` 绑定 verify；`effort=Max`、`threshold=Medium` 起步。
- 存量告警逐条处理：真问题就修；确认误报的用 `@SuppressFBWarnings` 注解或
  `spotbugs-exclude.xml` 豁免，**每条豁免必须附原因说明**。

### JaCoCo（覆盖率门禁）

- 实施时先实测当前行覆盖率，阈值取实测值向下取整到 5% 的整数倍
  （例如实测 63% → 阈值 60%），BUNDLE 级 LINE 计数器。
- 策略：只升不降；覆盖率每提升约 5% 手动上调阈值，阈值调整独立成 commit。

### Maven Enforcer（构建卫兵）

- `requireJavaVersion`：[17,)（构建 JDK，产物仍是 Java 8 字节码）。
- `requireMavenVersion`：[3.6.3,)。
- `banDuplicatePomDependencyVersions`：禁止重复依赖声明。
- 依赖收敛：优先启用 `dependencyConvergence`；若存量传递依赖冲突过多难以一次
  收敛，降级为 `requireUpperBoundDeps` 并在 POM 注释中记录该决定。

### 已知风险

- Groovy 3.0.17 在 JDK 17 下编译测试可能报错，届时将 Groovy 升至 3.0.19+
  （与 Spock 2.2-groovy-3.0 兼容）；如仍有问题，surefire/gmavenplus 增加
  必要的 `--add-opens` JVM 参数。
- Lombok 由 Spring Boot 2.7.12 管理（1.18.26），支持 JDK 17 构建，无需处理。

## 第 2 节 · CI（GitHub Actions）

单一 workflow：`.github/workflows/ci.yml`。

- 触发：push 到 master、所有 pull request。
- `concurrency`：同分支新提交取消进行中的旧运行。
- 步骤：
  1. `actions/checkout`
  2. `actions/setup-java`（Temurin 17，启用内置 Maven 依赖缓存）
  3. `mvn -B verify`（含 Spotless / SpotBugs / JaCoCo / Enforcer 全部门禁）
  4. 无论成败，上传 surefire 报告与 JaCoCo HTML 报告为 artifact（便于排查）。

## 第 3 节 · 测试 Harness（无 Docker，基于 H2）

### ArchUnit 架构守护

新增 Java 测试类 `ArchitectureTest`（JUnit 5 + ArchUnit），固化规则：

- `domain` 不依赖 api / application / config / infrastructure / presentation。
- `application` 仅依赖 `domain`（及自身、JDK、通用工具库）。
- `domain`、`application` 不得被反向依赖规则破坏：即二者不依赖
  `infrastructure` / `presentation` / `config`。
- `api` 包不依赖 `infrastructure` 内部实现。
- 全局禁止包循环（`slices().should().beFreeOfCycles()`）。

存量违规处理：能低成本修复的直接修复；确有历史包袱的用 ArchUnit
`FreezingArchRule` 冻结存量（violation store 提交到 git），只拦截新增违规。

### 测试数据 Builder（fixtures）

- 在 `src/test/` 下建立统一 fixtures 包（如
  `com.bananice.businesstracer.fixtures`）。
- 为最常构造的 3-5 个领域对象（告警规则、FlowLog 及关联对象等，以实际测试中
  出现频率为准）提供 Builder / 工厂方法，带合理默认值、支持局部覆写。
- 仅要求新测试使用；既有 Spec 顺手改到时迁移，不做专项回改。

既有 Spock + H2 测试体系保持不变。

## 第 4 节 · AI 编码协作 Harness

### CLAUDE.md（中文，约 100 行内）

内容：项目一句话定位；DDD 分层地图与依赖规则（与 ArchUnit 规则一致）；常用命令
速查（`mvn verify` 全量验证、跑单个 Spec 的命令、`mvn spotless:apply` 格式化、
查看 JaCoCo 报告路径）；约定（Spock Spec 命名、conventional commits、豁免
SpotBugs/ArchUnit 必须写原因）；门禁清单（提交前 verify 必须通过）。

### .claude/settings.json（提交到 git，团队共享）

- 权限白名单：放行 `mvn` 常用只构建命令与 git 只读命令，减少确认打断。
- hooks 配置（见下）。

### 格式化 hook（增益项，可降级）

- PostToolUse hook：Claude 编辑/写入 `*.java` 后，用 palantir-java-format
  standalone jar 只格式化被改动的那个文件（约 1 秒）。
- jar 由脚本首次从本地 Maven 仓库复制到 `.claude/tools/`（该目录加入
  `.gitignore`），脚本本身提交到 git。
- 降级路径：若 Windows 下 hook 不稳定，移除 hook，改为 CLAUDE.md 约定
  「提交前跑 `mvn spotless:apply`」+ CI 兜底。

## 交付顺序

1. 质量门禁 + 全量格式化（第 1 节）——含基线 commit 切分。
2. CI 上线（第 2 节）——此后每步都有 CI 保护。
3. 测试 harness（第 3 节）。
4. AI 协作设施（第 4 节）。

每步独立成 commit；全程 `mvn verify` 保持绿色。

## 验收标准

- 本地与 CI 上 `mvn verify` 一条命令跑通全部门禁且通过。
- 任意 Java 文件格式破坏、新增 SpotBugs 告警、覆盖率跌破阈值、违反分层规则，
  均导致构建失败。
- GitHub PR 页面可见 CI 状态；失败时可下载测试与覆盖率报告。
- CLAUDE.md 与 `.claude/settings.json` 就位，Claude Code 在本项目内执行常用
  构建命令无需逐条确认。
