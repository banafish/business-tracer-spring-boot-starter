package com.bananice.businesstracer.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Persistent Object for Detail Log Record
 */
@Data
@TableName("business_trace_detail")
public class DetailLogPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("business_id")
    private String businessId;

    @TableField("parent_node_id")
    private String parentNodeId;

    @TableField("content")
    private String content;

    @TableField("status")
    private String status;

    @TableField("create_time")
    private LocalDateTime createTime;
}
