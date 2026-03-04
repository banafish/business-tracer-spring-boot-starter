package com.bananice.businesstracer.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Persistent Object for Node Log Record
 */
@Data
@TableName("business_trace_node")
public class NodeLogPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("business_id")
    private String businessId;

    @TableField("code")
    private String code;

    @TableField("name")
    private String name;

    @TableField("trace_id")
    private String traceId;

    @TableField("node_id")
    private String nodeId;

    @TableField("parent_node_id")
    private String parentNodeId;

    @TableField("content")
    private String content;

    @TableField("app_name")
    private String appName;

    @TableField("status")
    private String status;

    @TableField("cost_time")
    private Long costTime;

    @TableField("exception")
    private String exception;

    @TableField("input_params")
    private String inputParams;

    @TableField("output_params")
    private String outputParams;

    @TableField("create_time")
    private LocalDateTime createTime;
}
