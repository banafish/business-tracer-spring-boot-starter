package com.bananice.businesstracer.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bananice.businesstracer.domain.model.FlowLog;
import com.bananice.businesstracer.domain.repository.FlowLogRepository;
import com.bananice.businesstracer.infrastructure.persistence.mapper.FlowLogMapper;
import com.bananice.businesstracer.infrastructure.persistence.po.FlowLogPO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Infrastructure implementation of the Flow Log Repository
 */
@Repository
@RequiredArgsConstructor
public class FlowLogRepositoryImpl implements FlowLogRepository {

    private final FlowLogMapper flowLogMapper;

    @Override
    public void save(FlowLog flowLog) {
        if (flowLog == null) {
            return;
        }
        FlowLogPO po = new FlowLogPO();
        BeanUtils.copyProperties(flowLog, po);
        flowLogMapper.insert(po);
    }

    @Override
    public boolean existsByFlowCodeAndBusinessId(String flowCode, String businessId) {
        QueryWrapper<FlowLogPO> query = new QueryWrapper<>();
        query.eq("flow_code", flowCode);
        query.eq("business_id", businessId);
        return flowLogMapper.selectCount(query) > 0;
    }

    @Override
    public List<FlowLog> findAll(String flowCode, String businessId, int pageNum, int pageSize) {
        QueryWrapper<FlowLogPO> query = new QueryWrapper<>();

        if (StringUtils.hasText(flowCode)) {
            query.eq("flow_code", flowCode);
        }
        if (StringUtils.hasText(businessId)) {
            query.eq("business_id", businessId);
        }

        query.orderByDesc("create_time");

        // Apply pagination
        int offset = (pageNum - 1) * pageSize;
        query.last("LIMIT " + offset + ", " + pageSize);

        List<FlowLogPO> pos = flowLogMapper.selectList(query);
        return pos.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long count(String flowCode, String businessId) {
        QueryWrapper<FlowLogPO> query = new QueryWrapper<>();

        if (StringUtils.hasText(flowCode)) {
            query.eq("flow_code", flowCode);
        }
        if (StringUtils.hasText(businessId)) {
            query.eq("business_id", businessId);
        }

        return flowLogMapper.selectCount(query);
    }

    @Override
    public void updateStatus(String flowCode, String businessId, String status) {
        if (!StringUtils.hasText(flowCode) || !StringUtils.hasText(businessId) || !StringUtils.hasText(status)) {
            return;
        }
        FlowLogPO updatePO = new FlowLogPO();
        updatePO.setStatus(status);

        QueryWrapper<FlowLogPO> query = new QueryWrapper<>();
        query.eq("flow_code", flowCode);
        query.eq("business_id", businessId);
        flowLogMapper.update(updatePO, query);
    }

    @Override
    public void updateStatusByBusinessId(String businessId, String status) {
        if (!StringUtils.hasText(businessId) || !StringUtils.hasText(status)) {
            return;
        }
        FlowLogPO updatePO = new FlowLogPO();
        updatePO.setStatus(status);

        QueryWrapper<FlowLogPO> query = new QueryWrapper<>();
        query.eq("business_id", businessId);
        flowLogMapper.update(updatePO, query);
    }

    @Override
    public List<FlowLog> findInProgressBefore(LocalDateTime threshold, int limit) {
        if (threshold == null || limit <= 0) {
            return Collections.emptyList();
        }
        QueryWrapper<FlowLogPO> query = new QueryWrapper<>();
        query.eq("status", "IN_PROGRESS")
                .le("create_time", threshold)
                .orderByAsc("create_time")
                .last("LIMIT " + limit);

        List<FlowLogPO> pos = flowLogMapper.selectList(query);
        return pos.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<FlowLog> findByCreateTimeBefore(LocalDateTime threshold, int limit) {
        if (threshold == null || limit <= 0) {
            return Collections.emptyList();
        }
        QueryWrapper<FlowLogPO> query = new QueryWrapper<>();
        query.le("create_time", threshold)
                .orderByAsc("create_time")
                .last("LIMIT " + limit);

        List<FlowLogPO> pos = flowLogMapper.selectList(query);
        return pos.stream().map(this::toDomain).collect(Collectors.toList());
    }

    private FlowLog toDomain(FlowLogPO po) {
        if (po == null) {
            return null;
        }
        return FlowLog.builder()
                .id(po.getId())
                .flowCode(po.getFlowCode())
                .name(po.getName())
                .businessId(po.getBusinessId())
                .status(po.getStatus())
                .createTime(po.getCreateTime())
                .build();
    }
}
