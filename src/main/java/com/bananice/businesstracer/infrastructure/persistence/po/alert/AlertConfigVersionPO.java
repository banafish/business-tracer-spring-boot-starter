package com.bananice.businesstracer.infrastructure.persistence.po.alert;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * Persistence object for business_alert_config_version table.
 */
@Data
@TableName("business_alert_config_version")
public class AlertConfigVersionPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("version_no")
    private Long versionNo;

    @TableField("checksum")
    private String checksum;

    @TableField("published")
    private Boolean published;
}
