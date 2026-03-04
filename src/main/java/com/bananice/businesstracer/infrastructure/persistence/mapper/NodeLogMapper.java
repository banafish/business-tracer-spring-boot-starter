package com.bananice.businesstracer.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bananice.businesstracer.infrastructure.persistence.po.NodeLogPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NodeLogMapper extends BaseMapper<NodeLogPO> {
}
