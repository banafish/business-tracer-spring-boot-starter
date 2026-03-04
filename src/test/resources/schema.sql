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
  PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `business_trace_detail` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `business_id` varchar(64) NOT NULL COMMENT 'Business ID',
  `parent_node_id` varchar(64) DEFAULT NULL COMMENT 'Parent Node ID',
  `content` text COMMENT 'Content',
  `status` varchar(20) DEFAULT 'NORMAL' COMMENT 'Status: NORMAL/FAILED',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
  PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `business_flow_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `flow_code` varchar(64) NOT NULL COMMENT 'Flow Code',
  `name` varchar(128) DEFAULT NULL COMMENT 'Flow Name',
  `business_id` varchar(64) NOT NULL COMMENT 'Business ID',
  `status` varchar(20) DEFAULT 'IN_PROGRESS' COMMENT 'Status: IN_PROGRESS, COMPLETED, FAILED',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
  PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `business_flow_dsl` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `flow_code` varchar(100) NOT NULL COMMENT 'DSL Unique Identifier',
  `name` varchar(200) NOT NULL COMMENT 'Business Flow Name',
  `layout` varchar(50) NOT NULL DEFAULT 'timeline' COMMENT 'Layout Type: tree/timeline/flow',
  `nodes_json` text COMMENT 'Node Configuration JSON',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Update Time',
  PRIMARY KEY (`id`)
);
