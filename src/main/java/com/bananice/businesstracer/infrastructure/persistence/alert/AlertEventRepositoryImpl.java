package com.bananice.businesstracer.infrastructure.persistence.alert;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bananice.businesstracer.domain.model.alert.AlertEvent;
import com.bananice.businesstracer.domain.model.alert.AlertStatus;
import com.bananice.businesstracer.domain.model.alert.AlertType;
import com.bananice.businesstracer.domain.repository.alert.AlertEventRepository;
import com.bananice.businesstracer.infrastructure.persistence.mapper.alert.AlertEventMapper;
import com.bananice.businesstracer.infrastructure.persistence.po.alert.AlertEventPO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class AlertEventRepositoryImpl implements AlertEventRepository {

    private final AlertEventMapper alertEventMapper;

    @Override
    public void save(AlertEvent alertEvent) {
        if (alertEvent == null) {
            return;
        }

        AlertEventPO po = new AlertEventPO();
        BeanUtils.copyProperties(alertEvent, po);
        po.setEventCode(UUID.randomUUID().toString().replace("-", ""));
        po.setAlertType(alertEvent.getAlertType() == null ? null : alertEvent.getAlertType().name());
        po.setStatus(alertEvent.getStatus() == null ? null : alertEvent.getStatus().name());
        po.setContent(alertEvent.getMessage());
        po.setLastOccurTime(alertEvent.getOccurredAt());
        po.setCreateTime(alertEvent.getOccurredAt());
        alertEventMapper.insert(po);
    }

    @Override
    public List<AlertEvent> query(LocalDateTime startTime, LocalDateTime endTime,
                                  AlertType alertType, AlertStatus status,
                                  String flowCode, String nodeCode, String businessId,
                                  int pageNum, int pageSize) {
        QueryWrapper<AlertEventPO> query = buildQuery(startTime, endTime, alertType, status, flowCode, nodeCode, businessId);
        query.orderByDesc("create_time");
        int offset = (pageNum - 1) * pageSize;
        query.last("LIMIT " + offset + ", " + pageSize);

        return alertEventMapper.selectList(query).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long count(LocalDateTime startTime, LocalDateTime endTime,
                      AlertType alertType, AlertStatus status,
                      String flowCode, String nodeCode, String businessId) {
        QueryWrapper<AlertEventPO> query = buildQuery(startTime, endTime, alertType, status, flowCode, nodeCode, businessId);
        return alertEventMapper.selectCount(query);
    }

    private QueryWrapper<AlertEventPO> buildQuery(LocalDateTime startTime, LocalDateTime endTime,
                                                  AlertType alertType, AlertStatus status,
                                                  String flowCode, String nodeCode, String businessId) {
        QueryWrapper<AlertEventPO> query = new QueryWrapper<>();
        if (startTime != null) {
            query.ge("create_time", startTime);
        }
        if (endTime != null) {
            query.le("create_time", endTime);
        }
        if (alertType != null) {
            query.eq("alert_type", alertType.name());
        }
        if (status != null) {
            query.eq("status", status.name());
        }
        if (StringUtils.hasText(flowCode)) {
            query.eq("flow_code", flowCode);
        }
        if (StringUtils.hasText(nodeCode)) {
            query.eq("node_code", nodeCode);
        }
        if (StringUtils.hasText(businessId)) {
            query.eq("business_id", businessId);
        }
        return query;
    }

    private AlertEvent toDomain(AlertEventPO po) {
        if (po == null) {
            return null;
        }
        AlertEvent alertEvent = AlertEvent.builder().build();
        BeanUtils.copyProperties(po, alertEvent);
        alertEvent.setAlertType(po.getAlertType() == null ? null : AlertType.valueOf(po.getAlertType()));
        alertEvent.setStatus(po.getStatus() == null ? null : AlertStatus.valueOf(po.getStatus()));
        alertEvent.setMessage(po.getContent());
        alertEvent.setOccurredAt(po.getCreateTime());
        return alertEvent;
    }
}
