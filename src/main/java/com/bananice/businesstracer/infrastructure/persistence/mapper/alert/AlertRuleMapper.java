package com.bananice.businesstracer.infrastructure.persistence.mapper.alert;

import com.bananice.businesstracer.infrastructure.persistence.po.alert.AlertRulePO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AlertRuleMapper extends BaseMapper<AlertRulePO> {}
