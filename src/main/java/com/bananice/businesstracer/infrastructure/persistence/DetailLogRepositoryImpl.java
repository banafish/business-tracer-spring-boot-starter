package com.bananice.businesstracer.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bananice.businesstracer.domain.model.DetailLog;
import com.bananice.businesstracer.domain.repository.DetailLogRepository;
import com.bananice.businesstracer.infrastructure.persistence.mapper.DetailLogMapper;
import com.bananice.businesstracer.infrastructure.persistence.po.DetailLogPO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class DetailLogRepositoryImpl implements DetailLogRepository {

    private final DetailLogMapper detailLogMapper;

    @Override
    public void save(DetailLog detailLog) {
        if (detailLog == null) {
            return;
        }
        DetailLogPO po = new DetailLogPO();
        BeanUtils.copyProperties(detailLog, po);
        detailLogMapper.insert(po);
    }

    @Override
    public List<DetailLog> findByParentNodeId(String parentNodeId) {
        QueryWrapper<DetailLogPO> query = new QueryWrapper<>();
        query.eq("parent_node_id", parentNodeId);
        query.orderByAsc("create_time");

        List<DetailLogPO> pos = detailLogMapper.selectList(query);
        return pos.stream().map(this::toDomain).collect(Collectors.toList());
    }

    private DetailLog toDomain(DetailLogPO po) {
        if (po == null) {
            return null;
        }
        DetailLog detailLog = DetailLog.builder().build();
        BeanUtils.copyProperties(po, detailLog);
        return detailLog;
    }
}
