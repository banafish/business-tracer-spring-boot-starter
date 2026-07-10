# 工程 Harness 落地实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 business-tracer-spring-boot-starter 落地方案 A 工程 harness：`mvn verify` 一条命令跑通全部质量门禁（Spotless/SpotBugs/JaCoCo/Enforcer），GitHub Actions CI，ArchUnit 架构守护 + 测试 fixtures，以及 CLAUDE.md + `.claude/` AI 协作设施。

**Architecture:** 所有质量门禁挂在 Maven `verify` 生命周期内，本地与 CI 行为完全一致；存量代码一次性清理到干净基线后严格阻断。架构规则用 ArchUnit 固化，存量违规用 FreezingArchRule 冻结。AI 协作层（CLAUDE.md、权限白名单、格式化 hook）只消费前面建好的构建设施，不引入新概念。

**Tech Stack:** Maven（JDK 21 构建 / Java 8 字节码）、Spotless + palantir-java-format、SpotBugs、JaCoCo、Maven Enforcer、GitHub Actions、ArchUnit 1.3、Spock/Groovy（既有）。

**对应规格:** `docs/superpowers/specs/2026-07-10-engineering-harness-design.md`

**环境事实（已确认）:**
- Maven 3.9.12，`JAVA_HOME=E:\program\jdk21`（Maven 用 JDK 21 跑；PATH 上的 `java` 是 JDK 8，与构建无关）。
- 基础包 `com.bananice.businesstracer`；分层包：api / application / config / domain / infrastructure / presentation。
- 已知存量分层违规（执行 ArchUnit 任务时将被冻结，不修复）：
  - `api/BusinessTracer.java` → `infrastructure.context.TraceContext`、`TraceContextHolder`
  - `application/alert/AlertDispatchService.java` → `infrastructure.alert.channel.AlertChannelSender`
- 工作目录是 Windows + PowerShell；计划中的命令均为 PowerShell 语法。

**版本注记:** 计划中固定的插件版本（spotless-maven-plugin 2.44.5、palantir-java-format 2.50.0、spotbugs-maven-plugin 4.8.6.6、jacoco 0.8.12、enforcer 3.5.0、archunit 1.3.0）若在 Maven Central 解析失败（构建会明确报 `Could not find artifact`），到 central.sonatype.com 查同 major 最新版替换后重跑该步骤，不改变其他配置。

---

### Task 1: 基线验证

**Files:** 无改动。

- [ ] **Step 1: 确认构建 JDK 与基线绿色**

Run: `mvn -B -version; mvn -B verify`
Expected: Maven 显示 `Java version: 21.x`；`BUILD SUCCESS`，全部既有 Spock 测试通过。

如果基线就是红的，停止并报告——后续任何任务都不得在红色基线上开始。例外：若失败原因是 Groovy 3.0.17 与 JDK 21 的兼容问题（gmavenplus 编译或 Spock 运行时报 `Unsupported class file major version` 之类），按规格的既定路径处理：把 `pom.xml` 中 groovy 依赖版本升到 `3.0.21`（与 Spock 2.2-groovy-3.0 兼容），重跑至绿色，单独提交 `build: bump groovy for jdk 21 compatibility`，再继续。

- [ ] **Step 2: 记录基线测试数量**

从输出中记下 `Tests run: N, Failures: 0` 的 N 值，后续任务完成后 N 只增不减。

---

### Task 2: 编译基线（release=8）+ Maven Enforcer

**Files:**
- Modify: `pom.xml`（properties 与 build/plugins）

- [ ] **Step 1: 修改 properties，新增 release 与插件版本属性**

`pom.xml` 的 `<properties>` 改为：

```xml
<properties>
    <java.version>1.8</java.version>
    <maven.compiler.release>8</maven.compiler.release>
    <mybatis-plus.version>3.5.3.1</mybatis-plus.version>
    <spotless-maven-plugin.version>2.44.5</spotless-maven-plugin.version>
    <palantir-java-format.version>2.50.0</palantir-java-format.version>
    <spotbugs-maven-plugin.version>4.8.6.6</spotbugs-maven-plugin.version>
    <jacoco-maven-plugin.version>0.8.12</jacoco-maven-plugin.version>
    <maven-enforcer-plugin.version>3.5.0</maven-enforcer-plugin.version>
    <archunit.version>1.3.0</archunit.version>
</properties>
```

（`java.version` 保留给 spring-boot-parent 的其他引用；`maven.compiler.release` 优先生效，用 JDK 21 编译并严格校验 Java 8 API 边界。spotless/spotbugs/jacoco 属性本任务先声明，后续任务使用。）

- [ ] **Step 2: 在 `<build><plugins>` 中追加 Enforcer 插件**

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-enforcer-plugin</artifactId>
    <version>${maven-enforcer-plugin.version}</version>
    <executions>
        <execution>
            <id>enforce-build-environment</id>
            <goals>
                <goal>enforce</goal>
            </goals>
            <configuration>
                <rules>
                    <requireJavaVersion>
                        <version>[17,)</version>
                        <message>构建需要 JDK 17+（产物仍是 Java 8 字节码，见 maven.compiler.release）</message>
                    </requireJavaVersion>
                    <requireMavenVersion>
                        <version>[3.6.3,)</version>
                    </requireMavenVersion>
                    <banDuplicatePomDependencyVersions/>
                    <dependencyConvergence/>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

- [ ] **Step 3: 运行验证，处理依赖收敛结果**

Run: `mvn -B verify`

- Expected（理想）: `BUILD SUCCESS`。
- 若 `dependencyConvergence` 报存量传递依赖冲突：冲突 ≤ 3 组时，在 `<dependencyManagement>` 中逐组固定版本（选各冲突组中最高版本）后重跑；冲突 > 3 组时，删除 `<dependencyConvergence/>`，替换为 `<requireUpperBoundDeps/>`，并在该行上方加 XML 注释：`<!-- 存量传递依赖冲突较多，暂用 upper-bound 检查代替完全收敛，见 2026-07-10 harness 规格 -->`，重跑至 `BUILD SUCCESS`。
- 若编译因 `release=8` 报错（代码误用了 Java 8 之后的 API）：这是真问题，修复该处代码用 Java 8 API 替代，重跑。

- [ ] **Step 4: Commit**

```powershell
git add pom.xml
git commit -m "build: compile with --release 8 and add maven-enforcer gates"
```

---

### Task 3: Spotless 引入 + 全量格式化

**Files:**
- Modify: `pom.xml`
- Create: `.mvn/jvm.config`
- Modify: 全部 `src/**/*.java`（约 96 个文件，纯格式化）
- Create: `.git-blame-ignore-revs`

- [ ] **Step 1: 创建 `.mvn/jvm.config`**

palantir-java-format 使用 javac 内部 API，JDK 16+ 需要 add-exports。创建 `.mvn/jvm.config`（无扩展名，内容如下，可多行）：

```
--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED
```

- [ ] **Step 2: 在 `<build><plugins>` 中追加 Spotless 插件**

```xml
<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <version>${spotless-maven-plugin.version}</version>
    <configuration>
        <java>
            <includes>
                <include>src/main/java/**/*.java</include>
                <include>src/test/java/**/*.java</include>
            </includes>
            <palantirJavaFormat>
                <version>${palantir-java-format.version}</version>
            </palantirJavaFormat>
            <importOrder/>
            <removeUnusedImports/>
            <trimTrailingWhitespace/>
            <endWithNewline/>
        </java>
        <pom>
            <sortPom>
                <expandEmptyElements>false</expandEmptyElements>
            </sortPom>
        </pom>
    </configuration>
    <executions>
        <execution>
            <id>spotless-check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <phase>validate</phase>
        </execution>
    </executions>
</plugin>
```

（Groovy Spec 不纳入格式化，规格已明确。）

- [ ] **Step 3: 全量格式化**

Run: `mvn -B spotless:apply`
Expected: `BUILD SUCCESS`，git status 显示大量 `.java` 与 `pom.xml` 被修改。

若报 `Could not find artifact com.diffplug.spotless...` 按头部「版本注记」换版本；若报 `IllegalAccessError ... com.sun.tools.javac`，确认 Step 1 的 `.mvn/jvm.config` 在仓库根目录且拼写正确。

- [ ] **Step 4: 验证全量构建仍绿**

Run: `mvn -B verify`
Expected: `BUILD SUCCESS`（含 spotless:check 通过），测试数量不少于 Task 1 记录的 N。

- [ ] **Step 5: 分两个 commit 提交（配置与格式化分离，保证 blame 可切割）**

```powershell
git add pom.xml .mvn/jvm.config
git commit -m "build: add spotless with palantir-java-format bound to verify"
git add -A
git commit -m "style: reformat all java sources with palantir-java-format"
```

（第一个 commit 单独 checkout 时 spotless:check 会红，这是刻意换取格式化 commit 的纯净性，可接受。）

- [ ] **Step 6: 把格式化 commit 加入 blame 忽略清单**

```powershell
$reformatHash = git rev-parse HEAD
"# 全量格式化 commit，git blame 时忽略`n$reformatHash" | Out-File -Encoding utf8 .git-blame-ignore-revs
git add .git-blame-ignore-revs
git commit -m "chore: add .git-blame-ignore-revs for bulk reformat"
```

---

### Task 4: SpotBugs 静态检查

**Files:**
- Modify: `pom.xml`
- Create: `spotbugs-exclude.xml`（仓库根目录）

- [ ] **Step 1: 创建 `spotbugs-exclude.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<FindBugsFilter>
    <!-- 豁免原因：项目大量使用 Lombok @Data/@Builder 的 PO/DTO 数据载体，
         EI_EXPOSE_REP/EI_EXPOSE_REP2（内部可变对象暴露）会被生成的 getter/setter
         海量触发，逐一防御式拷贝对数据载体收益极低。仅豁免这两类，其余保持严格。
         新增其他豁免时必须像本条一样写明原因。 -->
    <Match>
        <Bug pattern="EI_EXPOSE_REP"/>
    </Match>
    <Match>
        <Bug pattern="EI_EXPOSE_REP2"/>
    </Match>
</FindBugsFilter>
```

- [ ] **Step 2: 在 `<build><plugins>` 中追加 SpotBugs 插件**

```xml
<plugin>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-maven-plugin</artifactId>
    <version>${spotbugs-maven-plugin.version}</version>
    <configuration>
        <effort>Max</effort>
        <threshold>Medium</threshold>
        <excludeFilterFile>spotbugs-exclude.xml</excludeFilterFile>
    </configuration>
    <executions>
        <execution>
            <id>spotbugs-check</id>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

- [ ] **Step 3: 运行并处理存量告警**

Run: `mvn -B verify`

- Expected（理想）: `BUILD SUCCESS`。
- 若报存量告警：先用 `mvn -B spotbugs:gui` 以外的方式看明细——读 `target/spotbugsXml.xml` 或运行 `mvn -B spotbugs:check` 看控制台列表。逐条处理：
  - **真缺陷**（如空指针、资源未关闭、equals/hashCode 不一致）：直接修复代码。
  - **确认误报**：优先在该类/方法上加 `@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "PATTERN", justification = "原因")`（需在 pom 加依赖，见下），个别不便加注解的在 `spotbugs-exclude.xml` 加带原因注释的精确 `<Match><Class name="..."/><Bug pattern="..."/></Match>`。

  如需注解依赖，在 `<dependencies>` 中加：

```xml
<dependency>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-annotations</artifactId>
    <version>4.8.6</version>
    <scope>provided</scope>
    <optional>true</optional>
</dependency>
```

重跑至 `BUILD SUCCESS`。

- [ ] **Step 4: Commit**

```powershell
git add -A
git commit -m "build: add spotbugs static analysis gate (effort=max, threshold=medium)"
```

（若 Step 3 修了真缺陷，把缺陷修复拆成单独的 `fix:` commit 先提，再提本 commit。）

---

### Task 5: JaCoCo 覆盖率门禁

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: 在 `<build><plugins>` 中追加 JaCoCo 插件（阈值先占 0.00，下一步实测后回填）**

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>${jacoco-maven-plugin.version}</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check-coverage</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <!-- 策略：只升不降；每提升约 5% 手动上调，调整独立成 commit -->
                                <minimum>0.00</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

- [ ] **Step 2: 实测当前行覆盖率**

```powershell
mvn -B verify
$csv = Import-Csv target/site/jacoco/jacoco.csv
$missed  = ($csv | Measure-Object -Property LINE_MISSED  -Sum).Sum
$covered = ($csv | Measure-Object -Property LINE_COVERED -Sum).Sum
$pct = $covered / ($missed + $covered) * 100
"当前行覆盖率: {0:N2}%  -> 阈值取: {1}%" -f $pct, ([math]::Floor($pct / 5) * 5)
```

Expected: 输出实测覆盖率与向下取整到 5% 倍数的阈值（例如 `63.41% -> 60%`）。

- [ ] **Step 3: 回填阈值并验证**

把 Step 1 中 `<minimum>0.00</minimum>` 改为实测阈值（如 60% 写 `0.60`）。

Run: `mvn -B verify`
Expected: `BUILD SUCCESS`，日志含 `All coverage checks have been met`。

- [ ] **Step 4: Commit**

```powershell
git add pom.xml
git commit -m "build: add jacoco line coverage gate at measured baseline"
```

---

### Task 6: GitHub Actions CI

**Files:**
- Create: `.github/workflows/ci.yml`

- [ ] **Step 1: 创建 workflow**

```yaml
name: CI

on:
  push:
    branches: [master]
  pull_request:

concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

jobs:
  verify:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven

      - name: Build and verify (all quality gates)
        run: mvn -B -ntp verify

      - name: Upload test and coverage reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: reports
          path: |
            target/surefire-reports/
            target/site/jacoco/
          if-no-files-found: ignore
```

（CI 用 JDK 21 与本地一致；Enforcer 只要求 17+，规格允许。）

- [ ] **Step 2: Commit 并推送**

```powershell
git add .github/workflows/ci.yml
git commit -m "ci: add github actions workflow running mvn verify"
git push origin master
```

- [ ] **Step 3: 确认 CI 首跑绿色**

Run: `gh run watch --exit-status`（或 `gh run list --limit 1` 轮询直到 `completed`）
Expected: conclusion 为 `success`。

若失败：`gh run view --log-failed` 看日志。常见差异点是 Linux 行尾——若 spotless:check 在 CI 报行尾问题而本地绿，在仓库根目录添加 `.gitattributes`（内容：`* text=auto eol=lf`）并重新规范化（`git add --renormalize .`），作为 `chore:` commit 提交后重推。

---

### Task 7: ArchUnit 架构守护

**Files:**
- Modify: `pom.xml`（test 依赖）
- Create: `src/test/resources/archunit.properties`
- Create: `src/test/java/com/bananice/businesstracer/ArchitectureTest.java`
- Create: `src/test/resources/archunit-store/`（FreezingArchRule 自动生成，需提交）

- [ ] **Step 1: 添加 ArchUnit test 依赖**

`pom.xml` `<dependencies>` 中追加：

```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>${archunit.version}</version>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 2: 创建 `src/test/resources/archunit.properties`**

```properties
freeze.store.default.path=src/test/resources/archunit-store
freeze.store.default.allowStoreCreation=true
```

- [ ] **Step 3: 编写架构测试（先写测试——预期它会抓出已知存量违规）**

`src/test/java/com/bananice/businesstracer/ArchitectureTest.java`：

```java
package com.bananice.businesstracer;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

/**
 * DDD 分层守护。规则与 docs/superpowers/specs/2026-07-10-engineering-harness-design.md 一致。
 * 存量违规由 FreezingArchRule 冻结在 src/test/resources/archunit-store，只拦截新增违规；
 * 消除一条存量违规后重跑测试，store 会自动收缩，把收缩后的 store 一并提交。
 */
@AnalyzeClasses(packages = "com.bananice.businesstracer", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainDependsOnNothing = noClasses()
            .that()
            .resideInAPackage("..businesstracer.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "..businesstracer.api..",
                    "..businesstracer.application..",
                    "..businesstracer.config..",
                    "..businesstracer.infrastructure..",
                    "..businesstracer.presentation..");

    @ArchTest
    static final ArchRule applicationOnlyDependsOnDomain = FreezingArchRule.freeze(noClasses()
            .that()
            .resideInAPackage("..businesstracer.application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "..businesstracer.infrastructure..",
                    "..businesstracer.presentation..",
                    "..businesstracer.config.."));

    @ArchTest
    static final ArchRule apiDoesNotDependOnInfrastructure = FreezingArchRule.freeze(noClasses()
            .that()
            .resideInAPackage("..businesstracer.api..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..businesstracer.infrastructure.."));

    @ArchTest
    static final ArchRule noPackageCycles = FreezingArchRule.freeze(
            SlicesRuleDefinition.slices().matching("com.bananice.businesstracer.(*)").should().beFreeOfCycles());
}
```

- [ ] **Step 4: 首次运行——生成冻结 store**

Run: `mvn -B test "-Dtest=ArchitectureTest"`
Expected: PASS（FreezingArchRule 首次运行把存量违规写入 store 而不报错），且 `src/test/resources/archunit-store/` 下生成 `stored.rules` 与若干违规文件。检查 store 内容确实包含已知的两处违规（BusinessTracer→TraceContextHolder、AlertDispatchService→AlertChannelSender）。

`domainDependsOnNothing` 未加冻结——若它意外失败，说明 domain 层有未知的反向依赖，属于必须当场修复的真问题（把违规类的依赖改为 domain 内接口或移动类），不得冻结。

- [ ] **Step 5: 验证新增违规会被拦截（一次性演练，验证后还原）**

在任意 domain 类（如 `DslConfig.java`）临时加一行 `import com.bananice.businesstracer.infrastructure.registry.BusinessTraceRegistry;` 并声明一个该类型字段，运行 `mvn -B test "-Dtest=ArchitectureTest"`。
Expected: FAIL，报 domain 依赖 infrastructure。
还原改动（`git checkout -- src/main/java/com/bananice/businesstracer/domain/model/DslConfig.java`），重跑确认 PASS。

- [ ] **Step 6: 全量验证 + Commit**

```powershell
mvn -B verify
git add pom.xml src/test/resources/archunit.properties src/test/resources/archunit-store src/test/java/com/bananice/businesstracer/ArchitectureTest.java
git commit -m "test: add archunit layer guard with frozen legacy violations"
```

---

### Task 8: 测试数据 Fixtures

**Files:**
- Create: `src/test/groovy/com/bananice/businesstracer/fixtures/DomainFixtures.groovy`
- Test: `src/test/groovy/com/bananice/businesstracer/fixtures/DomainFixturesSpec.groovy`

既有各 Spec 内部大量重复 “Map defaults + overrides” 私有 helper（见 `AlertEvaluateServiceSpec.buildNodeLog`），本任务把该模式收敛为公共设施。默认值取自现有测试的惯用值。**不回改既有 Spec。**

- [ ] **Step 1: 先写失败测试**

`src/test/groovy/com/bananice/businesstracer/fixtures/DomainFixturesSpec.groovy`：

```groovy
package com.bananice.businesstracer.fixtures

import com.bananice.businesstracer.domain.model.alert.AlertStatus
import com.bananice.businesstracer.domain.model.alert.AlertType
import spock.lang.Specification

import java.time.LocalDateTime

class DomainFixturesSpec extends Specification {

    def "fixtures provide valid defaults"() {
        expect:
        with(DomainFixtures.anAlertRule()) {
            id != null && name && alertType != null && scopeType != null && enabled
        }
        with(DomainFixtures.anAlertChannel()) {
            id != null && name && channelType != null && target && enabled
        }
        with(DomainFixtures.anAlertEvent()) {
            id != null && ruleId != null && alertType != null && status == AlertStatus.NEW && occurredAt != null
        }
        with(DomainFixtures.aNodeLog()) {
            businessId && code && traceId && status && costTime != null && createTime != null
        }
        with(DomainFixtures.aFlowLog()) {
            flowCode && name && businessId && status && createTime != null
        }
    }

    def "fixtures merge overrides over defaults"() {
        when:
        def event = DomainFixtures.anAlertEvent(alertType: AlertType.FLOW_STUCK, businessId: "biz-x")
        def node = DomainFixtures.aNodeLog(createTime: LocalDateTime.of(2026, 1, 1, 0, 0))

        then:
        event.alertType == AlertType.FLOW_STUCK
        event.businessId == "biz-x"
        event.ruleId == 1L
        node.createTime == LocalDateTime.of(2026, 1, 1, 0, 0)
        node.code == "node-a"
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `mvn -B test "-Dtest=DomainFixturesSpec"`
Expected: 编译失败，`unable to resolve class ... DomainFixtures`。

- [ ] **Step 3: 实现 DomainFixtures**

`src/test/groovy/com/bananice/businesstracer/fixtures/DomainFixtures.groovy`：

```groovy
package com.bananice.businesstracer.fixtures

import com.bananice.businesstracer.domain.model.FlowLog
import com.bananice.businesstracer.domain.model.NodeLog
import com.bananice.businesstracer.domain.model.alert.AlertChannel
import com.bananice.businesstracer.domain.model.alert.AlertChannelType
import com.bananice.businesstracer.domain.model.alert.AlertEvent
import com.bananice.businesstracer.domain.model.alert.AlertRule
import com.bananice.businesstracer.domain.model.alert.AlertScopeType
import com.bananice.businesstracer.domain.model.alert.AlertStatus
import com.bananice.businesstracer.domain.model.alert.AlertType

import java.time.LocalDateTime

/**
 * 领域对象测试工厂：合理默认值 + Map 覆写。
 * 用法：DomainFixtures.anAlertEvent(alertType: AlertType.FLOW_STUCK)
 * 新测试一律用本类构造领域对象；既有 Spec 顺手改到时迁移。
 */
class DomainFixtures {

    static AlertRule anAlertRule(Map overrides = [:]) {
        def m = [
                id       : 1L,
                name     : "Node failed in payment",
                alertType: AlertType.NODE_FAILED,
                scopeType: AlertScopeType.NODE,
                scopeRef : "PAYMENT",
                enabled  : true,
        ] + overrides
        AlertRule.builder()
                .id(m.id as Long)
                .name(m.name as String)
                .alertType(m.alertType as AlertType)
                .scopeType(m.scopeType as AlertScopeType)
                .scopeRef(m.scopeRef as String)
                .enabled(m.enabled as boolean)
                .build()
    }

    static AlertChannel anAlertChannel(Map overrides = [:]) {
        def m = [
                id         : 2L,
                name       : "Ops WeCom",
                channelType: AlertChannelType.WECOM,
                target     : "wecom://group/ops",
                enabled    : true,
        ] + overrides
        AlertChannel.builder()
                .id(m.id as Long)
                .name(m.name as String)
                .channelType(m.channelType as AlertChannelType)
                .target(m.target as String)
                .enabled(m.enabled as boolean)
                .build()
    }

    static AlertEvent anAlertEvent(Map overrides = [:]) {
        def m = [
                id        : 3L,
                ruleId    : 1L,
                alertType : AlertType.NODE_FAILED,
                status    : AlertStatus.NEW,
                businessId: "biz-1",
                flowCode  : "order-flow",
                nodeCode  : "PAYMENT",
                traceId   : "trace-1",
                message   : "node failed",
                occurredAt: LocalDateTime.of(2026, 3, 22, 10, 0),
        ] + overrides
        AlertEvent.builder()
                .id(m.id as Long)
                .ruleId(m.ruleId as Long)
                .alertType(m.alertType as AlertType)
                .status(m.status as AlertStatus)
                .businessId(m.businessId as String)
                .flowCode(m.flowCode as String)
                .nodeCode(m.nodeCode as String)
                .traceId(m.traceId as String)
                .message(m.message as String)
                .occurredAt(m.occurredAt as LocalDateTime)
                .build()
    }

    static NodeLog aNodeLog(Map overrides = [:]) {
        def m = [
                businessId: "biz-1",
                code      : "node-a",
                traceId   : "trace-1",
                status    : "COMPLETED",
                costTime  : 100L,
                createTime: LocalDateTime.of(2026, 3, 22, 10, 0),
        ] + overrides
        NodeLog.builder()
                .businessId(m.businessId as String)
                .code(m.code as String)
                .traceId(m.traceId as String)
                .status(m.status as String)
                .costTime(m.costTime as Long)
                .createTime(m.createTime as LocalDateTime)
                .build()
    }

    static FlowLog aFlowLog(Map overrides = [:]) {
        def m = [
                flowCode  : "order-flow",
                name      : "Flow order-flow",
                businessId: "biz-1",
                status    : "IN_PROGRESS",
                createTime: LocalDateTime.of(2026, 3, 22, 10, 0),
        ] + overrides
        FlowLog.builder()
                .flowCode(m.flowCode as String)
                .name(m.name as String)
                .businessId(m.businessId as String)
                .status(m.status as String)
                .createTime(m.createTime as LocalDateTime)
                .build()
    }
}
```

（若某字段在实际 builder 上不存在导致编译错误，以领域类实际字段为准删改对应默认项，同时同步修改 Spec 断言——默认值全部来自既有测试的真实用法，预期不会发生。）

- [ ] **Step 4: 运行确认通过**

Run: `mvn -B test "-Dtest=DomainFixturesSpec"`
Expected: PASS，2 个测试通过。

- [ ] **Step 5: 全量验证 + Commit**

```powershell
mvn -B verify
git add src/test/groovy/com/bananice/businesstracer/fixtures/
git commit -m "test: add domain fixtures with defaults-plus-overrides pattern"
```

---

### Task 9: CLAUDE.md

**Files:**
- Create: `CLAUDE.md`（仓库根目录）

- [ ] **Step 1: 创建 CLAUDE.md，内容如下（阈值与命令若与实际执行结果不符，以实际为准修正）**

````markdown
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
````

- [ ] **Step 2: Commit**

```powershell
git add CLAUDE.md
git commit -m "docs: add CLAUDE.md project guide for ai coding agents"
```

---

### Task 10: .claude 配置（权限白名单 + 格式化 hook）

**Files:**
- Create: `.claude/settings.json`
- Create: `.claude/hooks/format-java.ps1`
- Create: `.claude/hooks/setup-format-tool.ps1`
- Modify: `.gitignore`（无则创建）

- [ ] **Step 1: 创建 `.claude/settings.json`**

```json
{
  "permissions": {
    "allow": [
      "Bash(mvn -B verify:*)",
      "Bash(mvn -B test:*)",
      "Bash(mvn -B clean:*)",
      "Bash(mvn -B compile:*)",
      "Bash(mvn -B spotless:apply)",
      "Bash(mvn -B spotless:check)",
      "Bash(mvn verify:*)",
      "Bash(mvn test:*)",
      "Bash(mvn spotless:apply)",
      "Bash(git status:*)",
      "Bash(git diff:*)",
      "Bash(git log:*)",
      "Bash(git show:*)",
      "Bash(git branch:*)"
    ]
  },
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Edit|Write",
        "hooks": [
          {
            "type": "command",
            "command": "powershell -NoProfile -ExecutionPolicy Bypass -File .claude/hooks/format-java.ps1"
          }
        ]
      }
    ]
  }
}
```

- [ ] **Step 2: 创建 `.claude/hooks/setup-format-tool.ps1`（一次性准备 formatter jar）**

```powershell
# 把 palantir-java-format 及其依赖复制到 .claude/tools/pjf（gitignored）。
# 只需在 clone 后执行一次；未执行时 format-java.ps1 会静默跳过。
$ErrorActionPreference = 'Stop'
$toolDir = Join-Path $PSScriptRoot '..\tools\pjf'
New-Item -ItemType Directory -Force $toolDir | Out-Null
$stubPom = Join-Path $env:TEMP 'pjf-stub-pom.xml'
@'
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>tmp</groupId>
  <artifactId>pjf-stub</artifactId>
  <version>1</version>
  <dependencies>
    <dependency>
      <groupId>com.palantir.javaformat</groupId>
      <artifactId>palantir-java-format</artifactId>
      <version>2.50.0</version>
    </dependency>
  </dependencies>
</project>
'@ | Out-File -Encoding utf8 $stubPom
mvn -q -f $stubPom dependency:copy-dependencies "-DoutputDirectory=$toolDir"
Write-Host "formatter jars ready in $toolDir"
```

（版本号与 pom.xml 中 `palantir-java-format.version` 保持一致；将来升级 pom 里的版本时同步改这里。）

- [ ] **Step 3: 创建 `.claude/hooks/format-java.ps1`**

```powershell
# PostToolUse hook：Claude 编辑/写入 .java 文件后，只格式化该文件（约 1 秒）。
# 任何前置条件不满足都静默退出（exit 0），绝不阻塞编辑；最终一致性由 mvn verify 的 spotless:check 兜底。
$ErrorActionPreference = 'SilentlyContinue'
$payload = [Console]::In.ReadToEnd() | ConvertFrom-Json
$file = $payload.tool_input.file_path
if (-not $file) { exit 0 }
if (-not $file.EndsWith('.java')) { exit 0 }
if (-not (Test-Path $file)) { exit 0 }
$toolDir = Join-Path $PSScriptRoot '..\tools\pjf'
if (-not (Test-Path $toolDir)) { exit 0 }
$java = Join-Path $env:JAVA_HOME 'bin\java.exe'
if (-not (Test-Path $java)) { exit 0 }
& $java `
  --add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED `
  --add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED `
  --add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED `
  --add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED `
  --add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED `
  --add-exports jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED `
  -cp "$toolDir\*" com.palantir.javaformat.java.Main --palantir --replace $file
exit 0
```

- [ ] **Step 4: `.gitignore` 追加 tools 目录**

在仓库根 `.gitignore`（无则创建）追加一行：

```
.claude/tools/
```

- [ ] **Step 5: 验证 hook 与 Spotless 输出一致（关键验证，决定 hook 去留）**

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .claude/hooks/setup-format-tool.ps1
# 用真实文件做等价验证：先确保工作区干净，然后用 hook 的方式格式化一个文件
git status --porcelain   # 应为空
$env:JAVA_HOME + '' | Out-Null
'{"tool_input":{"file_path":"src/main/java/com/bananice/businesstracer/application/DslService.java"}}' |
  powershell -NoProfile -ExecutionPolicy Bypass -File .claude/hooks/format-java.ps1
git status --porcelain   # 期望仍为空：spotless 已格式化过的文件，pjf 单文件格式化后应无 diff
mvn -B spotless:check    # 期望 BUILD SUCCESS
```

Expected: 两次 `git status --porcelain` 都为空，spotless:check 通过——证明 hook 与门禁用的是同一套格式。

**降级路径（规格已批准）**：若 `com.palantir.javaformat.java.Main` 不存在、`--palantir` 参数不被识别、或格式化产生了与 spotless 不一致的 diff（第二次 git status 非空）——执行 `git checkout -- .` 还原，删除 `.claude/settings.json` 中整个 `"hooks"` 节点，删除 `.claude/hooks/format-java.ps1` 与 `setup-format-tool.ps1`，在 CLAUDE.md「约定」末尾追加一行：`- 提交前先跑 mvn -B spotless:apply（无自动格式化 hook，CI spotless:check 兜底）。`

- [ ] **Step 6: Commit**

```powershell
git add .claude/settings.json .claude/hooks/ .gitignore
git commit -m "chore: add claude code settings with permission allowlist and format hook"
```

（若走了降级路径，改为提交实际保留的文件，commit message 用 `chore: add claude code settings with permission allowlist`。）

---

### Task 11: 收尾全量验证与推送

**Files:** 无新改动。

- [ ] **Step 1: 全量验证**

Run: `mvn -B clean verify`
Expected: `BUILD SUCCESS`；测试数量 ≥ Task 1 的 N + 3（新增 ArchitectureTest 的 4 条规则与 DomainFixturesSpec 的 2 个用例）。

- [ ] **Step 2: 对照验收标准自检（规格「验收标准」节）**

- 门禁齐备：故意破坏一处格式（如删一个换行）→ `mvn -B verify` 失败 → 还原。
- `.github/workflows/ci.yml`、`CLAUDE.md`、`.claude/settings.json` 均已提交。

- [ ] **Step 3: 推送并确认 CI 绿色**

```powershell
git push origin master
gh run watch --exit-status
```

Expected: CI `success`。

- [ ] **Step 4: 汇报**

向用户汇报：实测覆盖率与设定阈值、SpotBugs 存量告警处理明细（修复几处/豁免几处及原因）、Enforcer 依赖收敛采用了哪条规则、hook 是否走了降级路径。

---

## 实施偏差记录（2026-07-10 执行完毕后补记）

以下为执行过程中对计划文本的偏离，均保持规格意图：

1. **Lombok pin 1.18.30**（commit aa0ad50）：Boot 2.7.12 托管的 1.18.26 不支持 javac 21，clean 构建必炸；计划编写时被增量编译掩盖，Task 2 质量审查发现后补入。
2. **ArchUnit 切片模式 `(*)` → `(*)..`**（Task 7）：计划原模式只匹配顶层包直属类，实测抓不到任何嵌套包循环（与计划自身预期矛盾）；修正后真实冻结 4 组存量循环。
3. **CI 用 Temurin 21**（规格正文写 17）：与本地构建 JDK 一致；规格约束为"JDK 17+"，enforcer 下限仍为 [17,)（palantir-java-format 2.50.0 实测为 Java 11 字节码，无需收紧）。
4. **最终整体审查后的加固**（收尾 commit）：`archunit.properties` 的 `allowStoreCreation` 常态改为 `false`（防规则改动时静默重冻结，生成 store 需一次性 -D 开启）；application 分层规则补禁 `..api..`（无存量违规，store 随规则文本重生成）；pom 中 enforcer 移到 spotless 之前（环境卫兵先于格式化器报错）；`.gitignore` 补 `.claude/settings.local.json`。
5. **TDD 红灯形态**（Task 8）：Groovy 动态编译使"类不存在"表现为运行时 `MissingPropertyException` 而非计划预期的编译错误，根因相同。
6. **hook 走主路径**（Task 10）：palantir-java-format standalone CLI 与 Spotless 门禁输出逐字节一致（含 CRLF），降级路径未触发。
7. **已知限制**：CI 中 surefire 失败时 JaCoCo HTML 报告不会生成（report 绑 test 阶段、失败即中止），此场景下 artifact 只含 surefire 报告；覆盖率门禁失败场景报告必然存在，可接受。
