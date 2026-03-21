package com.bananice.businesstracer.infrastructure.persistence.alert;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bananice.businesstracer.domain.model.alert.AlertRule;
import com.bananice.businesstracer.domain.model.alert.AlertScopeType;
import com.bananice.businesstracer.domain.model.alert.AlertType;
import com.bananice.businesstracer.domain.repository.alert.AlertRuleRepository;
import com.bananice.businesstracer.infrastructure.persistence.mapper.alert.AlertRuleMapper;
import com.bananice.businesstracer.infrastructure.persistence.po.alert.AlertRulePO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class AlertRuleRepositoryImpl implements AlertRuleRepository {

    private final AlertRuleMapper alertRuleMapper;

    @Override
    public void save(AlertRule alertRule) {
        if (alertRule == null || alertRule.getScopeType() == null || !StringUtils.hasText(alertRule.getScopeRef())) {
            return;
        }

        AlertRulePO existing = selectByScope(alertRule.getScopeType(), alertRule.getFlowCode(), alertRule.getScopeRef());

        AlertRulePO po = new AlertRulePO();
        BeanUtils.copyProperties(alertRule, po);
        po.setRuleName(alertRule.getName());
        po.setAlertType(alertRule.getAlertType() == null ? null : alertRule.getAlertType().name());
        po.setScopeType(alertRule.getScopeType().name());
        po.setScopeCode(alertRule.getScopeRef());

        if (existing != null) {
            po.setId(existing.getId());
            po.setRuleCode(existing.getRuleCode());
            alertRuleMapper.updateById(po);
        } else {
            po.setRuleCode(UUID.randomUUID().toString().replace("-", ""));
            alertRuleMapper.insert(po);
        }
    }

    @Override
    public AlertRule findByScope(AlertScopeType scopeType, String flowCode, String scopeRef) {
        if (scopeType == null || !StringUtils.hasText(scopeRef)) {
            return null;
        }
        return toDomain(selectByScope(scopeType, flowCode, scopeRef));
    }

    @Override
    public List<AlertRule> findAll() {
        QueryWrapper<AlertRulePO> query = new QueryWrapper<>();
        query.orderByDesc("id");
        return alertRuleMapper.selectList(query).stream().map(this::toDomain).collect(Collectors.toList());
    }

    private AlertRulePO selectByScope(AlertScopeType scopeType, String flowCode, String scopeRef) {
        QueryWrapper<AlertRulePO> query = new QueryWrapper<>();
        query.eq("scope_type", scopeType.name());
        query.eq("scope_code", scopeRef);
        if (StringUtils.hasText(flowCode)) {
            query.eq("flow_code", flowCode);
        } else {
            query.isNull("flow_code");
        }
        return alertRuleMapper.selectOne(query);
    }

    private AlertRule toDomain(AlertRulePO po) {
        if (po == null) {
            return null;
        }
        AlertRule alertRule = AlertRule.builder().build();
        BeanUtils.copyProperties(po, alertRule);
        alertRule.setName(po.getRuleName());
        alertRule.setAlertType(po.getAlertType() == null ? null : AlertType.valueOf(po.getAlertType()));
        alertRule.setScopeType(po.getScopeType() == null ? null : AlertScopeType.valueOf(po.getScopeType()));
        alertRule.setScopeRef(po.getScopeCode());
        return alertRule;
    }
}
