CREATE TABLE IF NOT EXISTS `business_trace_node` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `business_id` varchar(64) NOT NULL COMMENT 'Business ID',
  `code` varchar(64) NOT NULL COMMENT 'Node Code',
  `name` varchar(128) DEFAULT NULL COMMENT 'Node Display Name',
  `trace_id` varchar(64) DEFAULT NULL COMMENT 'Trace ID',
  `node_id` varchar(64) NOT NULL COMMENT 'Node ID',
  `parent_node_id` varchar(64) DEFAULT NULL COMMENT 'Parent Node ID',
  `content` text COMMENT 'Content',
  `app_name` varchar(64) DEFAULT NULL COMMENT 'App Name',
  `status` varchar(20) DEFAULT 'COMPLETED' COMMENT 'Node Status (COMPLETED/FAILED)',
  `cost_time` bigint(20) DEFAULT 0 COMMENT 'Execution time in ms',
  `exception` text COMMENT 'Exception stack trace if any',
  `input_params` text COMMENT 'Resolved input parameters (JSON)',
  `output_params` text COMMENT 'Resolved output parameters (JSON)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
  PRIMARY KEY (`id`),
  KEY `idx_node_business_id` (`business_id`),
  KEY `idx_node_code` (`code`)
) COMMENT='Business Flow Node';

CREATE TABLE IF NOT EXISTS `business_trace_detail` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `business_id` varchar(64) NOT NULL COMMENT 'Business ID',
  `parent_node_id` varchar(64) DEFAULT NULL COMMENT 'Parent Node ID',
  `content` text COMMENT 'Content',
  `status` varchar(20) DEFAULT 'NORMAL' COMMENT 'Status: NORMAL/FAILED',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
  PRIMARY KEY (`id`),
  KEY `idx_detail_business_id` (`business_id`)
) COMMENT='Business Flow Detail';

CREATE TABLE IF NOT EXISTS `business_flow_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `flow_code` varchar(64) NOT NULL COMMENT 'Flow Code',
  `name` varchar(128) DEFAULT NULL COMMENT 'Flow Name',
  `business_id` varchar(64) NOT NULL COMMENT 'Business ID',
  `status` varchar(20) DEFAULT 'IN_PROGRESS' COMMENT 'Status: IN_PROGRESS, COMPLETED, FAILED',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_business` (`flow_code`, `business_id`)
);

CREATE TABLE IF NOT EXISTS `business_flow_dsl` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `flow_code` varchar(100) NOT NULL COMMENT 'DSL Unique Identifier',
  `name` varchar(200) NOT NULL COMMENT 'Business Flow Name',
  `layout` varchar(50) NOT NULL DEFAULT 'timeline' COMMENT 'Layout Type: tree/timeline/flow',
  `nodes_json` text COMMENT 'Node Configuration JSON',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_code` (`flow_code`)
);

CREATE TABLE IF NOT EXISTS `business_alert_rule` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `rule_code` varchar(64) NOT NULL COMMENT 'Rule Code',
  `rule_name` varchar(128) DEFAULT NULL COMMENT 'Rule Name',
  `alert_type` varchar(32) NOT NULL COMMENT 'Alert Type',
  `scope_type` varchar(16) NOT NULL COMMENT 'Scope Type: GLOBAL/FLOW/NODE',
  `scope_code` varchar(128) NOT NULL COMMENT 'Scope Code',
  `flow_code` varchar(64) DEFAULT NULL COMMENT 'Flow Code',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Enabled Flag',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_alert_rule_scope` (`scope_type`, `scope_code`),
  UNIQUE KEY `uk_alert_rule_scope_flow` (`scope_type`, `flow_code`, `scope_code`)
);

CREATE TABLE IF NOT EXISTS `business_alert_channel` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `channel_code` varchar(64) NOT NULL COMMENT 'Channel Code',
  `channel_type` varchar(32) NOT NULL COMMENT 'Channel Type',
  `channel_name` varchar(128) DEFAULT NULL COMMENT 'Channel Name',
  `config_json` text COMMENT 'Channel Config JSON',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Enabled Flag',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_alert_channel_code` (`channel_code`)
);

CREATE TABLE IF NOT EXISTS `business_alert_event` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `event_code` varchar(64) NOT NULL COMMENT 'Event Code',
  `alert_type` varchar(32) NOT NULL COMMENT 'Alert Type',
  `status` varchar(32) NOT NULL COMMENT 'Event Status',
  `aggregate_key` varchar(128) DEFAULT NULL COMMENT 'Aggregate Key',
  `flow_code` varchar(64) DEFAULT NULL COMMENT 'Flow Code',
  `node_code` varchar(64) DEFAULT NULL COMMENT 'Node Code',
  `business_id` varchar(64) DEFAULT NULL COMMENT 'Business ID',
  `content` text COMMENT 'Alert Content',
  `last_occur_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Last Occur Time',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_alert_event_code` (`event_code`),
  KEY `idx_alert_event_type_status_time` (`create_time`, `alert_type`, `status`),
  KEY `idx_alert_event_flow_node_biz_time` (`flow_code`, `node_code`, `business_id`, `create_time`),
  KEY `idx_alert_event_agg_status_occur` (`aggregate_key`, `status`, `last_occur_time`)
);

CREATE TABLE IF NOT EXISTS `business_alert_dispatch_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `event_id` bigint(20) NOT NULL COMMENT 'Alert Event ID',
  `channel_code` varchar(64) NOT NULL COMMENT 'Channel Code',
  `dispatch_status` varchar(32) NOT NULL COMMENT 'Dispatch Status',
  `dispatch_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Dispatch Time',
  `error_message` text COMMENT 'Error Message',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
  PRIMARY KEY (`id`),
  KEY `idx_alert_dispatch_event_time` (`event_id`, `dispatch_time`)
);

CREATE TABLE IF NOT EXISTS `business_alert_config_version` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `version_no` bigint(20) NOT NULL COMMENT 'Version Number',
  `checksum` varchar(128) DEFAULT NULL COMMENT 'Checksum',
  `published` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'Published Flag',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_alert_config_version_no` (`version_no`)
);
