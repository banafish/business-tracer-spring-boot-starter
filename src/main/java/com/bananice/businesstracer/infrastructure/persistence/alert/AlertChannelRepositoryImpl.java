package com.bananice.businesstracer.infrastructure.persistence.alert;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bananice.businesstracer.domain.model.alert.AlertChannel;
import com.bananice.businesstracer.domain.model.alert.AlertChannelType;
import com.bananice.businesstracer.domain.repository.alert.AlertChannelRepository;
import com.bananice.businesstracer.infrastructure.persistence.mapper.alert.AlertChannelMapper;
import com.bananice.businesstracer.infrastructure.persistence.po.alert.AlertChannelPO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class AlertChannelRepositoryImpl implements AlertChannelRepository {

    private final AlertChannelMapper alertChannelMapper;

    @Override
    public void save(AlertChannel alertChannel) {
        if (alertChannel == null) {
            return;
        }

        AlertChannelPO po = new AlertChannelPO();
        BeanUtils.copyProperties(alertChannel, po);
        po.setChannelType(alertChannel.getChannelType() == null ? null : alertChannel.getChannelType().name());
        po.setChannelName(alertChannel.getName());
        po.setConfigJson(alertChannel.getTarget());

        if (alertChannel.getId() != null) {
            AlertChannelPO existing = alertChannelMapper.selectById(alertChannel.getId());
            if (existing != null) {
                po.setChannelCode(existing.getChannelCode());
            }
            alertChannelMapper.updateById(po);
            return;
        }

        po.setChannelCode(UUID.randomUUID().toString().replace("-", ""));
        alertChannelMapper.insert(po);
    }

    @Override
    public List<AlertChannel> findEnabled() {
        QueryWrapper<AlertChannelPO> query = new QueryWrapper<>();
        query.eq("enabled", true);
        query.orderByAsc("id");

        return alertChannelMapper.selectList(query).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<AlertChannel> findAll() {
        QueryWrapper<AlertChannelPO> query = new QueryWrapper<>();
        query.orderByAsc("id");
        return alertChannelMapper.selectList(query).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public AlertChannel findById(Long id) {
        if (id == null) {
            return null;
        }
        return toDomain(alertChannelMapper.selectById(id));
    }

    private AlertChannel toDomain(AlertChannelPO po) {
        if (po == null) {
            return null;
        }
        AlertChannel alertChannel = AlertChannel.builder().build();
        BeanUtils.copyProperties(po, alertChannel);
        alertChannel.setName(po.getChannelName());
        alertChannel.setChannelType(po.getChannelType() == null ? null : AlertChannelType.valueOf(po.getChannelType()));
        alertChannel.setTarget(po.getConfigJson());
        return alertChannel;
    }
}
