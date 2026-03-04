package com.bananice.businesstracer.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bananice.businesstracer.infrastructure.persistence.po.FlowLogPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper for Flow Log
 */
@Mapper
public interface FlowLogMapper extends BaseMapper<FlowLogPO> {
}
