# Alert Center Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a full monitoring/alerting center for business-tracer-spring-boot-starter with DB-backed rules/channels/history, immediate-effect config updates, realtime+aggregation dispatch, and a web UI to manage everything.

**Architecture:** Add a new alert bounded context following existing DDD layering (`domain` + `application` + `infrastructure` + `presentation`). Reuse existing async execution infrastructure and MyBatis-Plus repository style. Keep runtime behavior non-intrusive to tracing flow (alert failures never break business tracing), and apply rule resolution `NODE(flow+node) > FLOW(flow) > GLOBAL` with hot cache refresh.

**Tech Stack:** Spring Boot 2.7, MyBatis-Plus, Lombok, H2/MySQL SQL scripts, Spock (Groovy) for tests, built-in static HTML/CSS/JS pages under `META-INF/resources/business-tracer`.

---

## File Structure Map

### New/Modified Database Scripts
- Modify: `sql/init.sql`
  - Add production alert tables/indexes (`business_alert_rule`, `business_alert_channel`, `business_alert_event`, `business_alert_dispatch_log`, `business_alert_config_version`) and cleanup-related indexes.
- Modify: `src/test/resources/schema.sql`
  - Mirror alert tables for H2 tests.

### New Domain Models/Repositories
- Create: `src/main/java/com/bananice/businesstracer/domain/model/alert/AlertType.java`
- Create: `src/main/java/com/bananice/businesstracer/domain/model/alert/AlertStatus.java`
- Create: `src/main/java/com/bananice/businesstracer/domain/model/alert/AlertScopeType.java`
- Create: `src/main/java/com/bananice/businesstracer/domain/model/alert/AlertChannelType.java`
- Create: `src/main/java/com/bananice/businesstracer/domain/model/alert/AlertRule.java`
- Create: `src/main/java/com/bananice/businesstracer/domain/model/alert/AlertChannel.java`
- Create: `src/main/java/com/bananice/businesstracer/domain/model/alert/AlertEvent.java`
- Create: `src/main/java/com/bananice/businesstracer/domain/model/alert/AlertDispatchLog.java`
- Create: `src/main/java/com/bananice/businesstracer/domain/model/alert/AlertContext.java`
- Create: `src/main/java/com/bananice/businesstracer/domain/repository/alert/AlertRuleRepository.java`
- Create: `src/main/java/com/bananice/businesstracer/domain/repository/alert/AlertChannelRepository.java`
- Create: `src/main/java/com/bananice/businesstracer/domain/repository/alert/AlertEventRepository.java`
- Create: `src/main/java/com/bananice/businesstracer/domain/repository/alert/AlertDispatchLogRepository.java`
- Create: `src/main/java/com/bananice/businesstracer/domain/repository/alert/AlertConfigVersionRepository.java`

### New Infrastructure Persistence
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/persistence/po/alert/AlertRulePO.java`
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/persistence/po/alert/AlertChannelPO.java`
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/persistence/po/alert/AlertEventPO.java`
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/persistence/po/alert/AlertDispatchLogPO.java`
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/persistence/po/alert/AlertConfigVersionPO.java`
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/persistence/mapper/alert/AlertRuleMapper.java`
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/persistence/mapper/alert/AlertChannelMapper.java`
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/persistence/mapper/alert/AlertEventMapper.java`
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/persistence/mapper/alert/AlertDispatchLogMapper.java`
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/persistence/mapper/alert/AlertConfigVersionMapper.java`
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/persistence/alert/AlertRuleRepositoryImpl.java`
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/persistence/alert/AlertChannelRepositoryImpl.java`
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/persistence/alert/AlertEventRepositoryImpl.java`
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/persistence/alert/AlertDispatchLogRepositoryImpl.java`
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/persistence/alert/AlertConfigVersionRepositoryImpl.java`

### New Application Services/DTOs
- Create: `src/main/java/com/bananice/businesstracer/application/alert/AlertRuleResolveService.java`
- Create: `src/main/java/com/bananice/businesstracer/application/alert/AlertConfigCacheService.java`
- Create: `src/main/java/com/bananice/businesstracer/application/alert/AlertEvaluateService.java`
- Create: `src/main/java/com/bananice/businesstracer/application/alert/AlertDispatchService.java`
- Create: `src/main/java/com/bananice/businesstracer/application/alert/AlertAggregationService.java`
- Create: `src/main/java/com/bananice/businesstracer/application/alert/AlertHistoryService.java`
- Create: `src/main/java/com/bananice/businesstracer/application/alert/AlertChannelService.java`
- Create: `src/main/java/com/bananice/businesstracer/application/alert/AlertRuleService.java`
- Create: `src/main/java/com/bananice/businesstracer/application/dto/alert/*.java` (request/response/page DTOs)

### New Alert Delivery + Scheduled Jobs
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/alert/channel/AlertChannelSender.java`
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/alert/channel/WebhookAlertChannelSender.java`
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/alert/channel/WecomAlertChannelSender.java`
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/alert/channel/DingtalkAlertChannelSender.java`
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/alert/channel/EmailAlertChannelSender.java`
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/alert/job/FlowStuckScanJob.java`
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/alert/job/AlertAggregationFlushJob.java`
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/alert/job/AlertHistoryCleanupJob.java`
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/alert/job/AlertConfigSyncJob.java`

### New/Modified Controllers and Config
- Create: `src/main/java/com/bananice/businesstracer/presentation/http/AlertRuleController.java`
- Create: `src/main/java/com/bananice/businesstracer/presentation/http/AlertChannelController.java`
- Create: `src/main/java/com/bananice/businesstracer/presentation/http/AlertHistoryController.java`
- Modify: `src/main/java/com/bananice/businesstracer/config/BusinessTracerProperties.java`
  - Add alert defaults (threshold/window/retention) and scheduler intervals.
- Modify: `src/main/java/com/bananice/businesstracer/config/BusinessTracerAutoConfiguration.java`
  - Ensure scheduled tasks enabled and alert beans wired.
- Modify: `src/main/java/com/bananice/businesstracer/infrastructure/aspect/BusinessTraceAspect.java`
  - Keep existing trace behavior; do not publish alerts directly.
- Modify: `src/main/java/com/bananice/businesstracer/application/TraceAsyncLogService.java`
  - Canonical alert producer: after NodeLog persisted, evaluate and emit alert events.
  - Add idempotency guard keyed by `(nodeId, alertType)` to avoid duplicate event creation.
- (Optional small refactor) Modify: `src/main/java/com/bananice/businesstracer/application/FlowLogService.java`
  - Add helper query for in-progress timeout scanning.

### Frontend
- Create: `src/main/resources/META-INF/resources/business-tracer/alerts.html`
- Create: `src/main/resources/META-INF/resources/business-tracer/alerts.js`
- Modify: `src/main/resources/META-INF/resources/business-tracer/sidebar.js`
  - Add nav entry for alert center.
- (Optional) Modify: `src/main/resources/META-INF/resources/business-tracer/common.css`
  - Small shared styles for alert page components.

### Tests
- Create: `src/test/groovy/com/bananice/businesstracer/application/alert/AlertRuleResolveServiceSpec.groovy`
- Create: `src/test/groovy/com/bananice/businesstracer/application/alert/AlertEvaluateServiceSpec.groovy`
- Create: `src/test/groovy/com/bananice/businesstracer/application/alert/AlertAggregationServiceSpec.groovy`
- Create: `src/test/groovy/com/bananice/businesstracer/application/alert/AlertConfigCacheServiceSpec.groovy`
- Create: `src/test/groovy/com/bananice/businesstracer/infrastructure/persistence/alert/AlertRuleRepositoryImplSpec.groovy`
- Create: `src/test/groovy/com/bananice/businesstracer/infrastructure/persistence/alert/AlertEventRepositoryImplSpec.groovy`
- Create: `src/test/groovy/com/bananice/businesstracer/presentation/http/AlertControllerSpec.groovy`
- Modify: `src/test/groovy/com/bananice/businesstracer/infrastructure/aspect/BusinessTraceAspectSpec.groovy`
  - Add integration assertions for alert event creation path.

### Docs (Optional)
- Modify: `README.md`
- Modify: `README_zh.md`
  - Add alert center usage and configuration examples.

---

### Task 1: Extend SQL schema for alert domain

**Files:**
- Modify: `sql/init.sql`
- Modify: `src/test/resources/schema.sql`
- Test: `src/test/groovy/com/bananice/businesstracer/infrastructure/persistence/alert/AlertSchemaSqlSpec.groovy`

- [ ] **Step 1: Write failing SQL-level schema test (no repository dependency)**

```groovy
def "alert tables and indexes exist in test schema"() {
    expect:
    // query H2 metadata for business_alert_* tables and key indexes
}
```

- [ ] **Step 2: Run test to verify schema-related failure**

Run: `mvn -pl business-tracer-spring-boot-starter -Dtest=AlertSchemaSqlSpec test`
Expected: FAIL due to missing alert tables/indexes.

- [ ] **Step 3: Add alert tables/indexes/version table in SQL scripts**

Add DDL for:
- `business_alert_rule`
- `business_alert_channel`
- `business_alert_event`
- `business_alert_dispatch_log`
- `business_alert_config_version`

Include indexes from spec and cleanup-friendly keys.

- [ ] **Step 4: Re-run targeted schema test**

Run: `mvn -pl business-tracer-spring-boot-starter -Dtest=AlertSchemaSqlSpec test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add business-tracer-spring-boot-starter/sql/init.sql business-tracer-spring-boot-starter/src/test/resources/schema.sql business-tracer-spring-boot-starter/src/test/groovy/com/bananice/businesstracer/infrastructure/persistence/alert/AlertSchemaSqlSpec.groovy
git commit -m "feat: add alert domain database schema"
```

### Task 2: Build alert domain model and repository interfaces

**Files:**
- Create: `src/main/java/com/bananice/businesstracer/domain/model/alert/*.java`
- Create: `src/main/java/com/bananice/businesstracer/domain/repository/alert/*.java`
- Test: `src/test/groovy/com/bananice/businesstracer/domain/model/alert/AlertDomainContractSpec.groovy`

- [ ] **Step 1: Write failing domain-contract test using new domain objects**

```groovy
def "alert scope and channel enums expose expected constants"() {}
def "alert entities keep required fields"() {}
```

- [ ] **Step 2: Run test to verify missing types fail**

Run: `mvn -pl business-tracer-spring-boot-starter -Dtest=AlertDomainContractSpec test`
Expected: FAIL with compile/type not found.

- [ ] **Step 3: Implement minimal enums/entities/interfaces**

Create focused types only needed by resolver and later services:
- scope/channel/type/status enums
- rule/channel/event/dispatch domain entities
- repository interfaces

- [ ] **Step 4: Re-run domain-contract test**

Run: `mvn -pl business-tracer-spring-boot-starter -Dtest=AlertDomainContractSpec test`
Expected: PASS.

- [ ] **Step 5: Commit (only after tests pass)**

```bash
git add business-tracer-spring-boot-starter/src/main/java/com/bananice/businesstracer/domain/model/alert business-tracer-spring-boot-starter/src/main/java/com/bananice/businesstracer/domain/repository/alert business-tracer-spring-boot-starter/src/test/groovy/com/bananice/businesstracer/domain/model/alert/AlertDomainContractSpec.groovy
git commit -m "feat: add alert domain contracts"
```

### Task 3: Implement alert persistence layer (PO/Mapper/RepositoryImpl)

**Files:**
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/persistence/po/alert/*.java`
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/persistence/mapper/alert/*.java`
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/persistence/alert/*.java`
- Test: `src/test/groovy/com/bananice/businesstracer/infrastructure/persistence/alert/AlertRuleRepositoryImplSpec.groovy`
- Test: `src/test/groovy/com/bananice/businesstracer/infrastructure/persistence/alert/AlertEventRepositoryImplSpec.groovy`

- [ ] **Step 1: Write failing persistence tests for CRUD + filtered query + upsert semantics**

```groovy
def "saveRule upserts by scope and flow/node uniqueness"() {}
def "queryEvents supports time and type filters with pagination"() {}
```

- [ ] **Step 2: Run tests and confirm failures**

Run: `mvn -pl business-tracer-spring-boot-starter -Dtest=AlertRuleRepositoryImplSpec,AlertEventRepositoryImplSpec test`
Expected: FAIL due to missing implementation.

- [ ] **Step 3: Implement repository impls matching existing style (BeanUtils + QueryWrapper)**

Keep methods minimal and explicit; no speculative abstractions.

- [ ] **Step 4: Re-run persistence tests**

Run: same as Step 2.
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add business-tracer-spring-boot-starter/src/main/java/com/bananice/businesstracer/infrastructure/persistence business-tracer-spring-boot-starter/src/test/groovy/com/bananice/businesstracer/infrastructure/persistence/alert
git commit -m "feat: implement alert repositories"
```

### Task 4: Implement rule resolution and config cache with version sync

**Files:**
- Create: `src/main/java/com/bananice/businesstracer/application/alert/AlertRuleResolveService.java`
- Create: `src/main/java/com/bananice/businesstracer/application/alert/AlertConfigCacheService.java`
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/alert/job/AlertConfigSyncJob.java`
- Modify: `src/main/java/com/bananice/businesstracer/config/BusinessTracerProperties.java`
- Modify: `src/main/java/com/bananice/businesstracer/config/BusinessTracerAutoConfiguration.java`
- Test: `src/test/groovy/com/bananice/businesstracer/application/alert/AlertRuleResolveServiceSpec.groovy`
- Test: `src/test/groovy/com/bananice/businesstracer/application/alert/AlertConfigCacheServiceSpec.groovy`

- [ ] **Step 1: Add failing tests for precedence, local immediate refresh, and version-triggered sync**

```groovy
def "resolve uses NODE(flow+node) then FLOW then GLOBAL"() {}
def "save config refreshes local cache immediately"() {}
def "cache refreshes when config_version changes"() {}
```

- [ ] **Step 2: Run failing tests**

Run: `mvn -pl business-tracer-spring-boot-starter -Dtest=AlertRuleResolveServiceSpec,AlertConfigCacheServiceSpec test`
Expected: FAIL.

- [ ] **Step 3: Implement minimal resolver + cache + sync job**

- cache keeps compiled rule map
- save path bumps `business_alert_config_version`
- save path synchronously refreshes local cache before API success return
- sync job polls version and refreshes local cache for cross-instance convergence

- [ ] **Step 4: Re-run targeted tests**

Run: same as Step 2.
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add business-tracer-spring-boot-starter/src/main/java/com/bananice/businesstracer/application/alert business-tracer-spring-boot-starter/src/main/java/com/bananice/businesstracer/infrastructure/alert/job/AlertConfigSyncJob.java business-tracer-spring-boot-starter/src/main/java/com/bananice/businesstracer/config/BusinessTracerProperties.java business-tracer-spring-boot-starter/src/main/java/com/bananice/businesstracer/config/BusinessTracerAutoConfiguration.java business-tracer-spring-boot-starter/src/test/groovy/com/bananice/businesstracer/application/alert

git commit -m "feat: add alert rule resolver and hot cache sync"
```

### Task 5: Implement event evaluation, realtime dispatch, and aggregation

**Files:**
- Create: `src/main/java/com/bananice/businesstracer/application/alert/AlertEvaluateService.java`
- Create: `src/main/java/com/bananice/businesstracer/application/alert/AlertDispatchService.java`
- Create: `src/main/java/com/bananice/businesstracer/application/alert/AlertAggregationService.java`
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/alert/channel/*.java`
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/alert/job/AlertAggregationFlushJob.java`
- Test: `src/test/groovy/com/bananice/businesstracer/application/alert/AlertEvaluateServiceSpec.groovy`
- Test: `src/test/groovy/com/bananice/businesstracer/application/alert/AlertAggregationServiceSpec.groovy`
- Test: `src/test/groovy/com/bananice/businesstracer/application/alert/AlertDispatchServiceSpec.groovy`

- [ ] **Step 1: Write failing tests for type decisions, dedup lifecycle, silence boundaries, and failure isolation**

```groovy
def "node failed produces NODE_FAILED event"() {}
def "slow node triggers when costTime > threshold"() {}
def "stuck flow updates existing open incident instead of duplicating"() {}
def "stuck incident closes when flow becomes COMPLETED or FAILED"() {}
def "silence window across midnight suppresses alerts correctly at boundaries"() {}
def "channel timeout/retry is bounded and sender failure does not break pipeline"() {}
```

- [ ] **Step 2: Run tests and confirm failure**

Run: `mvn -pl business-tracer-spring-boot-starter -Dtest=AlertEvaluateServiceSpec,AlertAggregationServiceSpec,AlertDispatchServiceSpec test`
Expected: FAIL.

- [ ] **Step 3: Implement minimal evaluation + dispatch + aggregation**

- dispatch logs each channel attempt
- channel sender registry by `AlertChannelType`
- aggregation by dedup key, 5m flush
- `FLOW_STUCK` lifecycle includes open/update/close transitions
- silence window supports cross-midnight boundaries
- timeout/retry has strict upper bound and failures are isolated

- [ ] **Step 4: Re-run tests**

Run: same as Step 2.
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add business-tracer-spring-boot-starter/src/main/java/com/bananice/businesstracer/application/alert business-tracer-spring-boot-starter/src/main/java/com/bananice/businesstracer/infrastructure/alert business-tracer-spring-boot-starter/src/test/groovy/com/bananice/businesstracer/application/alert

git commit -m "feat: implement alert evaluate, dispatch and aggregation"
```

### Task 6: Integrate alert pipeline into tracing runtime and stuck-flow scan

**Files:**
- Modify: `src/main/java/com/bananice/businesstracer/infrastructure/aspect/BusinessTraceAspect.java`
- Modify: `src/main/java/com/bananice/businesstracer/application/TraceAsyncLogService.java`
- Modify: `src/main/java/com/bananice/businesstracer/application/FlowLogService.java`
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/alert/job/FlowStuckScanJob.java`
- Test: `src/test/groovy/com/bananice/businesstracer/infrastructure/aspect/BusinessTraceAspectSpec.groovy`

- [ ] **Step 1: Add failing integration assertions for single-producer and idempotency**

```groovy
def "FAILED node writes one NODE_FAILED alert event"() {}
def "slow node writes one SLOW_NODE event"() {}
def "single traced invocation does not create duplicate alert events"() {}
```

- [ ] **Step 2: Run targeted integration test and observe failure**

Run: `mvn -pl business-tracer-spring-boot-starter -Dtest=BusinessTraceAspectSpec test`
Expected: FAIL for missing alert behavior.

- [ ] **Step 3: Implement integration hooks with minimal coupling**

- `BusinessTraceAspect` only produces node log context (no direct alert publish)
- `TraceAsyncLogService` is canonical alert producer after `nodeLogRepository.save`
- add idempotency guard `(nodeId, alertType)`
- add stuck scan job using `IN_PROGRESS` + create_time threshold

- [ ] **Step 4: Re-run integration test**

Run: same as Step 2.
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add business-tracer-spring-boot-starter/src/main/java/com/bananice/businesstracer/infrastructure/aspect/BusinessTraceAspect.java business-tracer-spring-boot-starter/src/main/java/com/bananice/businesstracer/application/TraceAsyncLogService.java business-tracer-spring-boot-starter/src/main/java/com/bananice/businesstracer/application/FlowLogService.java business-tracer-spring-boot-starter/src/main/java/com/bananice/businesstracer/infrastructure/alert/job/FlowStuckScanJob.java business-tracer-spring-boot-starter/src/test/groovy/com/bananice/businesstracer/infrastructure/aspect/BusinessTraceAspectSpec.groovy

git commit -m "feat: hook alert pipeline into trace runtime"
```

### Task 7: Build alert management APIs (rules/channels/history/test-send)

**Files:**
- Create: `src/main/java/com/bananice/businesstracer/presentation/http/AlertRuleController.java`
- Create: `src/main/java/com/bananice/businesstracer/presentation/http/AlertChannelController.java`
- Create: `src/main/java/com/bananice/businesstracer/presentation/http/AlertHistoryController.java`
- Create: `src/main/java/com/bananice/businesstracer/application/dto/alert/*.java`
- Test: `src/test/groovy/com/bananice/businesstracer/presentation/http/AlertControllerSpec.groovy`

- [ ] **Step 1: Write failing controller tests for endpoints and validation**

```groovy
def "PUT NODE rule without flowCode returns 400"() {}
def "POST test-send dispatches one test event"() {}
```

- [ ] **Step 2: Run failing API tests**

Run: `mvn -pl business-tracer-spring-boot-starter -Dtest=AlertControllerSpec test`
Expected: FAIL.

- [ ] **Step 3: Implement controllers and DTO mapping**

Return `ApiResult` wrapper consistently with existing controllers.

- [ ] **Step 4: Re-run API tests**

Run: same as Step 2.
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add business-tracer-spring-boot-starter/src/main/java/com/bananice/businesstracer/presentation/http/Alert* business-tracer-spring-boot-starter/src/main/java/com/bananice/businesstracer/application/dto/alert business-tracer-spring-boot-starter/src/test/groovy/com/bananice/businesstracer/presentation/http/AlertControllerSpec.groovy

git commit -m "feat: add alert management APIs"
```

### Task 8: Build frontend alert center page and navigation

**Files:**
- Create: `src/main/resources/META-INF/resources/business-tracer/alerts.html`
- Create: `src/main/resources/META-INF/resources/business-tracer/alerts.js`
- Modify: `src/main/resources/META-INF/resources/business-tracer/sidebar.js`
- Modify: `src/main/resources/META-INF/resources/business-tracer/common.css` (if needed)
- Test: manual UI checks + API smoke through browser

- [ ] **Step 1: Write minimal UI behavior checklist as executable manual test cases**

```text
- Rules tab: load tree and save global/flow/node rule
- Channels tab: create webhook channel and test-send
- Silence tab: edit silence windows and persist
- History tab: filter by alert type/time and open dispatch log detail
```

- [ ] **Step 2: Run app and verify page missing/failing state first**

Run: `mvn -pl business-tracer-spring-boot-starter spring-boot:run`
Expected: `alerts.html` not found or incomplete.

- [ ] **Step 3: Implement page with existing UI patterns**

- add `告警中心` nav item
- 4 tabs with API calls and form validation
- avoid framework addition; plain HTML/JS like existing pages

- [ ] **Step 4: Re-run manual checks and capture pass/fail notes**

Expected: all checklist items pass.

- [ ] **Step 5: Commit**

```bash
git add business-tracer-spring-boot-starter/src/main/resources/META-INF/resources/business-tracer/alerts.html business-tracer-spring-boot-starter/src/main/resources/META-INF/resources/business-tracer/alerts.js business-tracer-spring-boot-starter/src/main/resources/META-INF/resources/business-tracer/sidebar.js business-tracer-spring-boot-starter/src/main/resources/META-INF/resources/business-tracer/common.css

git commit -m "feat: add alert center frontend"
```

### Task 9: Add cleanup job and full verification

**Files:**
- Create: `src/main/java/com/bananice/businesstracer/infrastructure/alert/job/AlertHistoryCleanupJob.java`
- Modify: `src/test/resources/application.yml` (if scheduler knobs needed for tests)

- [ ] **Step 1: Write failing test for retention cleanup behavior**

```groovy
def "cleanup job deletes events and dispatch logs older than retention days"() {}
```

- [ ] **Step 2: Run targeted test and observe failure**

Run: `mvn -pl business-tracer-spring-boot-starter -Dtest=AlertEventRepositoryImplSpec test`
Expected: FAIL for missing cleanup path.

- [ ] **Step 3: Implement cleanup job**

- scheduled cleanup by retention days (default 30)
- delete `dispatch_log` first, then `event`

- [ ] **Step 4: Run full verification suite**

Run:
- `mvn -pl business-tracer-spring-boot-starter test`
- `mvn -pl business-tracer-spring-boot-starter -Dtest=BusinessTraceAspectSpec,AlertControllerSpec,AlertRuleResolveServiceSpec test`

Expected: all PASS.

- [ ] **Step 5: Commit**

```bash
git add business-tracer-spring-boot-starter/src/main/java/com/bananice/businesstracer/infrastructure/alert/job/AlertHistoryCleanupJob.java business-tracer-spring-boot-starter/src/test/resources/application.yml

git commit -m "feat: add alert retention cleanup"
```

### Task 10 (Optional): Update README docs

**Files:**
- Modify: `README.md`
- Modify: `README_zh.md`

- [ ] **Step 1: Add alert center usage snippets and config examples**

- [ ] **Step 2: Preview markdown and ensure examples match implemented APIs**

- [ ] **Step 3: Commit docs-only update**

```bash
git add business-tracer-spring-boot-starter/README.md business-tracer-spring-boot-starter/README_zh.md

git commit -m "docs: add alert center usage guide"
```

---

## Final Verification Checklist (must pass before branch completion)

- [ ] `NODE(flow+node) > FLOW(flow) > GLOBAL` precedence verified by tests
- [ ] Realtime + 5-minute aggregation verified by tests
- [ ] `FLOW_STUCK` dedup lifecycle verified (single open incident, update not duplicate)
- [ ] `FLOW_STUCK` close lifecycle verified when flow becomes `COMPLETED/FAILED`
- [ ] Single producer + idempotency verified (one trace execution does not duplicate alert events)
- [ ] Save-immediate-effect verified on single instance (no polling delay)
- [ ] Config version sync verified (multi-instance propagation logic path)
- [ ] Silence window boundary cases verified (跨午夜 + 边界值)
- [ ] Channel timeout/retry bounded and failure isolation verified
- [ ] Frontend 4 tabs functional against APIs
- [ ] 30-day retention cleanup verified
- [ ] Existing tracing features regression-free

## Execution Notes

- Keep each task small and commit after each passing slice.
- Prefer minimal implementation that satisfies tests (YAGNI).
- Reuse existing patterns from `Dsl*` and `FlowLog*` modules for consistency.
- If any task grows beyond a focused 2-5 minute action sequence, split it before coding.
