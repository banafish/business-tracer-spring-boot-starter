package com.bananice.businesstracer.infrastructure.persistence.mapper.alert;

import com.bananice.businesstracer.infrastructure.persistence.po.alert.AlertDispatchLogPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AlertDispatchLogMapper extends BaseMapper<AlertDispatchLogPO> {}
