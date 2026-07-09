package com.bananice.businesstracer.infrastructure.persistence.mapper;

import com.bananice.businesstracer.infrastructure.persistence.po.NodeLogPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NodeLogMapper extends BaseMapper<NodeLogPO> {}
