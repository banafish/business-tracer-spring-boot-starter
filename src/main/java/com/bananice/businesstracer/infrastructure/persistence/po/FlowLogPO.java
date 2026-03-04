package com.bananice.businesstracer.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Persistent Object for Flow Log
 */
@Data
@TableName("business_flow_log")
public class FlowLogPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("flow_code")
    private String flowCode;

    @TableField("name")
    private String name;

    @TableField("business_id")
    private String businessId;

    @TableField("status")
    private String status;

    @TableField("create_time")
    private LocalDateTime createTime;
}
