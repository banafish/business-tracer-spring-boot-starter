package com.bananice.businesstracer.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bananice.businesstracer.domain.model.NodeLog;
import com.bananice.businesstracer.domain.repository.NodeLogRepository;
import com.bananice.businesstracer.infrastructure.persistence.mapper.NodeLogMapper;
import com.bananice.businesstracer.infrastructure.persistence.po.NodeLogPO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class NodeLogRepositoryImpl implements NodeLogRepository {

    private final NodeLogMapper nodeLogMapper;

    @Override
    public void save(NodeLog nodeLog) {
        if (nodeLog == null) {
            return;
        }
        NodeLogPO po = new NodeLogPO();
        BeanUtils.copyProperties(nodeLog, po);
        nodeLogMapper.insert(po);
    }

    @Override
    public List<NodeLog> findByBusinessId(String businessId) {
        QueryWrapper<NodeLogPO> query = new QueryWrapper<>();
        query.eq("business_id", businessId);
        query.orderByAsc("create_time");

        List<NodeLogPO> pos = nodeLogMapper.selectList(query);
        return pos.stream().map(this::toDomain).collect(Collectors.toList());
    }

    private NodeLog toDomain(NodeLogPO po) {
        if (po == null) {
            return null;
        }
        NodeLog nodeLog = NodeLog.builder().build();
        BeanUtils.copyProperties(po, nodeLog);
        return nodeLog;
    }
}
