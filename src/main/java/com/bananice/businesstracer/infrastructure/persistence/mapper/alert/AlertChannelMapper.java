package com.bananice.businesstracer.infrastructure.persistence.mapper.alert;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bananice.businesstracer.infrastructure.persistence.po.alert.AlertChannelPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AlertChannelMapper extends BaseMapper<AlertChannelPO> {
}
