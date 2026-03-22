package com.bananice.businesstracer.infrastructure.persistence.po.alert;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Persistence object for business_alert_dispatch_log table.
 */
@Data
@TableName("business_alert_dispatch_log")
public class AlertDispatchLogPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("event_id")
    private Long eventId;

    @TableField("channel_code")
    private String channelCode;

    @TableField("dispatch_status")
    private String dispatchStatus;

    @TableField("dispatch_time")
    private LocalDateTime dispatchTime;

    @TableField("error_message")
    private String errorMessage;
}
