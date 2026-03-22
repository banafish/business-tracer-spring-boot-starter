# Business Tracer 监控告警能力设计（含前端配置中心）

## 1. 背景与目标

在 `business-tracer-spring-boot-starter` 中新增一套可落地、可配置、可视化运维的监控告警能力，满足以下要求：

- 告警类型：
  - 节点失败告警（`@BusinessTrace` 异常、`BusinessTracer.recordError`）
  - 慢调用告警（节点执行耗时超阈值）
  - 流转卡住告警（`business_flow_log` 长时间 `IN_PROGRESS`）
- 通知通道默认支持：
  - Webhook
  - 企业微信
  - 钉钉
  - 邮件
- 触发策略：实时 + 聚合
  - 实时即时发送
  - 5 分钟聚合汇总
- 默认阈值（保守）：
  - 慢调用 > 2000ms
  - 卡住流 > 10 分钟
- 配置覆盖粒度：
  - `global + flowCode + nodeCode`（优先级 `node > flow > global`）
- 额外要求（关键新增）：
  - 提供前端配置页面
  - 配置保存到数据库
  - 保存即生效
  - 提供告警历史、详情、测试发送、静默时间段
  - 历史保留 30 天

## 2. 范围与非目标

### 2.1 范围

- 新增告警中心页面（配置、通道、静默、历史）
- 新增告警域模型、应用服务、基础设施发送器
- 新增告警相关 DB 表与清理任务
- 在现有追踪链路中接入告警事件发布
- 新增告警查询与配置管理 REST API

### 2.2 非目标

- 不改造现有 DSL 设计与链路可视化主功能
- 不引入外部 MQ（先以内存 + 异步执行器为主）
- 不做多租户隔离（后续可扩展）

## 3. 现状与接入点

结合现有代码结构：

- 自动配置入口：`config/BusinessTracerAutoConfiguration.java`
- 核心追踪切面：`infrastructure/aspect/BusinessTraceAspect.java`
- 异步日志落库：`application/TraceAsyncLogService.java`
- 流状态处理：`application/FlowLogService.java`
- 现有前端模式：`resources/business-tracer/*.html + sidebar.js + common.css`
- 现有 DSL CRUD 架构：`presentation/http/DslController.java` + `application/DslService.java` + `domain.repository` + `infrastructure.persistence`

告警能力将沿用同样分层风格，保持一致性。

## 4. 方案对比与选型

### 方案 A（选中）内置告警引擎 + 前端告警中心 + DB 持久化

- 优点：功能完整，开箱即用，与你的目标完全一致
- 缺点：新增模块和测试面较大

### 方案 B：仅记录事件，外部系统发送

- 优点：starter 简洁
- 缺点：不能满足“默认多通道 + 前端管理 + 保存即生效”

### 方案 C：单表 JSON 配置快速版

- 优点：实现快
- 缺点：查询和局部更新能力差，可维护性弱

**结论：采用方案 A。**

## 5. 架构设计

### 5.1 分层与模块

新增 `alert` 子域（命名可按现有 package 风格对齐）：

- `domain.alert`
  - 告警规则模型（覆盖/继承）
  - 告警事件模型（含聚合键、状态）
  - 通道配置模型
- `application.alert`
  - `AlertRuleService`（规则解析、覆盖合并）
  - `AlertEvaluateService`（事件判定）
  - `AlertDispatchService`（分发编排）
  - `AlertHistoryService`（历史查询）
  - `AlertConfigCacheService`（热加载缓存）
- `infrastructure.alert`
  - 各通道 sender（Webhook/WeCom/DingTalk/Email）
  - 定时任务（卡住流扫描、聚合flush、历史清理）
  - repository impl / mapper / po
- `presentation.http`
  - `AlertConfigController`
  - `AlertHistoryController`
  - `AlertChannelController`

### 5.2 事件入口

1. 节点失败 / 慢调用（实时）
   - 在 `BusinessTraceAspect` 节点结束后（已可得到 `costTime`、`status`）发布候选事件
2. `recordError` 间接失败
   - 已通过 `TraceContext#errorRecorded` 汇总到 `BusinessTraceAspect`，同入口处理
3. 卡住流（定时）
   - 扫描 `business_flow_log.status=IN_PROGRESS` 且超阈值，发布 `FLOW_STUCK` 事件

### 5.3 规则优先级与判定

- 规则查找优先级（精确到流程上下文）：
  1) `NODE(flowCode + nodeCode)`
  2) `FLOW(flowCode)`
  3) `GLOBAL(*)`
- 规则项：
  - enabled
  - slowThresholdMs
  - stuckThresholdMinutes
  - channels
  - aggregationWindow
  - silenceWindows
- 判定流程：
  - 事件 -> 取生效规则 -> 静默过滤 -> 生成实时发送任务 -> 写入聚合桶

### 5.4 实时 + 聚合

- 实时：事件命中即发
- 聚合：按 `dedupKey(type+flowCode+nodeCode+appName)` 在 5 分钟窗口汇总
- 窗口到期发汇总消息（含次数、首次/末次时间）
- 事件幂等与重告警（尤其 `FLOW_STUCK`）：
  - 同一 `(businessId, flowCode)` 仅维护一个“未恢复”卡住事件
  - 定时扫描再次命中时，不重复新建事件，而是更新 `aggregate_count/last_occur_time`
  - 发送策略：首次命中实时发送，后续按聚合窗口发送提醒
  - 当流状态变为 `COMPLETED/FAILED` 时关闭该卡住事件

### 5.5 保存即生效

- API 保存配置入库成功后，主动触发 `AlertConfigCache` 刷新
- 规则读取优先走内存缓存
- 增加定时兜底刷新，避免缓存漂移
- 多实例一致性：
  - 引入“配置版本号（config_version）+ 短周期轮询”机制
  - 每实例本地缓存记录当前版本，轮询发现版本变化立即全量/增量刷新
  - 目标传播时延：`<= 5s`（作为验收指标）
- 结果：无需重启，下一条事件即用新配置（集群内在传播时延内达成一致）

## 6. 前端告警中心设计

### 6.1 导航与页面

- 新增导航项：`告警中心`
- 新增页面：`/business-tracer/alerts.html`
- 风格沿用 `common.css + sidebar.js`

### 6.2 页面信息架构（4 Tab）

1. 规则配置
   - 左侧规则树：Global / Flow / Node
   - 右侧规则编辑：失败/慢/卡住阈值、窗口、通道、启用
   - 支持“重置为上级继承”
2. 通道配置
   - Webhook / 企业微信 / 钉钉 / 邮件
   - 每类可多条配置
   - 支持测试发送
3. 静默时间段
   - 支持全局/flow/node
   - 支持按告警类型静默
   - 通过规则编辑 API 持久化（作为规则字段的一部分），并提供独立 UI 管理视图
4. 告警历史
   - 筛选 + 列表 + 事件详情 + 分发日志

## 7. 数据库设计

新增以下表（命名以 `business_alert_*` 前缀）：

### 7.1 `business_alert_rule`

- `id` bigint pk
- `scope_type` varchar(16)  // GLOBAL/FLOW/NODE
- `scope_code` varchar(128) // GLOBAL=*, FLOW=flowCode, NODE=nodeCode
- `flow_code` varchar(64)   // NODE 规则时必填；FLOW/GLOBAL 可空
- `enabled` tinyint
- `rule_json` text           // 阈值、窗口、静默、通道等
- `create_time` datetime
- `update_time` datetime
- unique key:
  - (`scope_type`, `scope_code`) for GLOBAL/FLOW
  - (`scope_type`, `flow_code`, `scope_code`) for NODE（确保 node 规则在 flow 维度精确命中）

### 7.2 `business_alert_channel`

- `id` bigint pk
- `channel_type` varchar(16) // WEBHOOK/WECOM/DINGTALK/EMAIL
- `channel_name` varchar(128)
- `enabled` tinyint
- `config_json` text
- `create_time` datetime
- `update_time` datetime

### 7.3 `business_alert_event`

- `id` bigint pk
- `alert_type` varchar(32)   // NODE_FAILED/SLOW_NODE/FLOW_STUCK
- `severity` varchar(16)
- `status` varchar(16)       // NEW/SENT/FAILED/SUPPRESSED
- `business_id` varchar(64)
- `flow_code` varchar(64)
- `node_code` varchar(64)
- `app_name` varchar(64)
- `message` text
- `aggregate_key` varchar(256)
- `aggregate_count` int
- `first_occur_time` datetime
- `last_occur_time` datetime
- `create_time` datetime

### 7.4 `business_alert_dispatch_log`

- `id` bigint pk
- `event_id` bigint
- `channel_id` bigint
- `dispatch_status` varchar(16)
- `request_payload` text
- `response_payload` text
- `error_msg` text
- `dispatch_time` datetime

### 7.5 索引与历史保留策略

- 建议索引：
  - `business_alert_event(create_time, alert_type, status)`
  - `business_alert_event(flow_code, node_code, business_id, create_time)`
  - `business_alert_event(aggregate_key, status, last_occur_time)`
  - `business_alert_dispatch_log(event_id, dispatch_time)`
- 定时清理 30 天前 `business_alert_event` 与关联 `dispatch_log`
  - 清理顺序：先删 `dispatch_log`，再删 `event`，避免孤儿数据

## 8. API 设计

### 8.1 规则配置

- `GET /business-tracer/api/alerts/rules`
- `PUT /business-tracer/api/alerts/rules/{scopeType}/{scopeCode}`
  - 当 `scopeType=NODE` 时，请求体必须包含 `flowCode`，用于精确命中

### 8.2 通道配置

- `GET /business-tracer/api/alerts/channels`
- `POST /business-tracer/api/alerts/channels`
- `PUT /business-tracer/api/alerts/channels/{id}`
- `POST /business-tracer/api/alerts/channels/{id}/test-send`

### 8.3 告警历史

- `GET /business-tracer/api/alerts/events`（分页 + 多条件过滤）
- `GET /business-tracer/api/alerts/events/{id}`
- `GET /business-tracer/api/alerts/events/{id}/dispatch-logs`

## 9. 配置模型（外部配置）

支持可选 YAML 默认值（作为初始化兜底）：

```yaml
business-tracer:
  alert:
    enabled: true
    realtime:
      enabled: true
    aggregation:
      enabled: true
      window: 5m
    defaults:
      slow-threshold-ms: 2000
      stuck-threshold-minutes: 10
      retain-days: 30
```

运行时以 DB 配置为准；若 DB 无记录则回落默认值。

## 10. 错误处理与安全

- 告警发送失败不影响业务主链路（仅记录失败）
- 所有外部请求设置超时与重试上限（防阻塞）
- 前端输入做基础校验，后端做最终校验
- 密钥类配置可在 `config_json` 内进行脱敏回显

## 11. 测试策略

### 11.1 单元测试

- 规则覆盖优先级（node > flow > global）
- 阈值判断（失败/慢/卡住）
- 静默窗口判定
- 聚合窗口与 dedup 逻辑

### 11.2 集成测试

- 保存配置后即时生效
- 多实例配置传播时延（目标 <= 5s）
- 多通道分发与失败记录
- 卡住流扫描触发与重告警抑制
- 历史查询过滤与分页

### 11.3 回归测试

- 现有 DSL/Trace UI 不回归
- 现有日志落库/流状态更新不回归

## 12. 验收标准

- [ ] 三类告警可触发并可区分
- [ ] 四种通道可配置且可测试发送
- [ ] 实时 + 5 分钟聚合均生效
- [ ] 支持 global/flow/node 覆盖并按优先级命中
- [ ] 前端页面可完成全流程配置与查询
- [ ] 保存即生效（无需重启）
- [ ] 多实例配置传播时延 <= 5s
- [ ] 告警历史可查，30 天清理生效

## 13. 实施注意事项

- 沿用现有 DDD 与前端组织方式，避免风格漂移
- 控制新增抽象层级，优先可读与可维护
- 避免对已有链路注入侵入式逻辑（通过应用服务解耦）

---

该设计已覆盖：三类告警、四通道、实时+聚合、DB 持久化、前端配置中心、保存即生效、历史管理与清理策略。
