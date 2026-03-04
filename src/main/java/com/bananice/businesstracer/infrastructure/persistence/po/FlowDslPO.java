package com.bananice.businesstracer.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Persistence Object for business_flow_dsl table
 */
@Data
@TableName("business_flow_dsl")
public class FlowDslPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * DSL unique identifier
     */
    private String flowCode;

    /**
     * Business flow name
     */
    private String name;

    /**
     * Layout type: tree/timeline/flow
     */
    private String layout;

    /**
     * Node configuration JSON
     */
    private String nodesJson;

    /**
     * Creation time
     */
    private LocalDateTime createTime;

    /**
     * Update time
     */
    private LocalDateTime updateTime;
}
