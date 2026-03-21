package com.bananice.businesstracer.infrastructure.persistence.alert;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bananice.businesstracer.domain.model.alert.AlertDispatchLog;
import com.bananice.businesstracer.domain.model.alert.AlertStatus;
import com.bananice.businesstracer.domain.repository.alert.AlertDispatchLogRepository;
import com.bananice.businesstracer.infrastructure.persistence.mapper.alert.AlertDispatchLogMapper;
import com.bananice.businesstracer.infrastructure.persistence.po.alert.AlertDispatchLogPO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class AlertDispatchLogRepositoryImpl implements AlertDispatchLogRepository {

    private final AlertDispatchLogMapper alertDispatchLogMapper;

    @Override
    public void save(AlertDispatchLog alertDispatchLog) {
        if (alertDispatchLog == null) {
            return;
        }

        AlertDispatchLogPO po = new AlertDispatchLogPO();
        BeanUtils.copyProperties(alertDispatchLog, po);
        po.setChannelCode(alertDispatchLog.getChannelId() == null ? null : String.valueOf(alertDispatchLog.getChannelId()));
        po.setDispatchStatus(alertDispatchLog.getStatus() == null ? null : alertDispatchLog.getStatus().name());
        po.setErrorMessage(alertDispatchLog.getResponse());
        alertDispatchLogMapper.insert(po);
    }

    @Override
    public List<AlertDispatchLog> findByEventId(Long eventId) {
        QueryWrapper<AlertDispatchLogPO> query = new QueryWrapper<>();
        query.eq("event_id", eventId);
        query.orderByAsc("dispatch_time");

        return alertDispatchLogMapper.selectList(query).stream().map(this::toDomain).collect(Collectors.toList());
    }

    private AlertDispatchLog toDomain(AlertDispatchLogPO po) {
        if (po == null) {
            return null;
        }
        AlertDispatchLog alertDispatchLog = AlertDispatchLog.builder().build();
        BeanUtils.copyProperties(po, alertDispatchLog);
        alertDispatchLog.setChannelId(po.getChannelCode() == null ? null : Long.valueOf(po.getChannelCode()));
        alertDispatchLog.setStatus(po.getDispatchStatus() == null ? null : AlertStatus.valueOf(po.getDispatchStatus()));
        alertDispatchLog.setResponse(po.getErrorMessage());
        return alertDispatchLog;
    }
}
