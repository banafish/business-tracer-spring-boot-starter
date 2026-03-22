package com.bananice.businesstracer.infrastructure.persistence.po.alert;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Persistence object for business_alert_event table.
 */
@Data
@TableName("business_alert_event")
public class AlertEventPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("event_code")
    private String eventCode;

    @TableField("alert_type")
    private String alertType;

    @TableField("status")
    private String status;

    @TableField("aggregate_key")
    private String aggregateKey;

    @TableField("flow_code")
    private String flowCode;

    @TableField("node_code")
    private String nodeCode;

    @TableField("business_id")
    private String businessId;

    @TableField("content")
    private String content;

    @TableField("last_occur_time")
    private LocalDateTime lastOccurTime;

    @TableField("create_time")
    private LocalDateTime createTime;
}
