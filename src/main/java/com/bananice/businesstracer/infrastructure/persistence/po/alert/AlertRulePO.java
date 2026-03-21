package com.bananice.businesstracer.infrastructure.persistence.po.alert;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * Persistence object for business_alert_rule table.
 */
@Data
@TableName("business_alert_rule")
public class AlertRulePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("rule_code")
    private String ruleCode;

    @TableField("rule_name")
    private String ruleName;

    @TableField("alert_type")
    private String alertType;

    @TableField("scope_type")
    private String scopeType;

    @TableField("scope_code")
    private String scopeCode;

    @TableField("flow_code")
    private String flowCode;

    @TableField("enabled")
    private Boolean enabled;
}
