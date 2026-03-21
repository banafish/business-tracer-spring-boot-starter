package com.bananice.businesstracer.infrastructure.persistence.po.alert;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * Persistence object for business_alert_channel table.
 */
@Data
@TableName("business_alert_channel")
public class AlertChannelPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("channel_code")
    private String channelCode;

    @TableField("channel_type")
    private String channelType;

    @TableField("channel_name")
    private String channelName;

    @TableField("config_json")
    private String configJson;

    @TableField("enabled")
    private Boolean enabled;
}
