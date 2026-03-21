package com.bananice.businesstracer.infrastructure.persistence.alert;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bananice.businesstracer.domain.repository.alert.AlertConfigVersionRepository;
import com.bananice.businesstracer.infrastructure.persistence.mapper.alert.AlertConfigVersionMapper;
import com.bananice.businesstracer.infrastructure.persistence.po.alert.AlertConfigVersionPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AlertConfigVersionRepositoryImpl implements AlertConfigVersionRepository {

    private final AlertConfigVersionMapper alertConfigVersionMapper;

    @Override
    public Long getPublishedVersion() {
        QueryWrapper<AlertConfigVersionPO> query = new QueryWrapper<>();
        query.eq("published", true);
        query.orderByDesc("version_no");
        query.last("LIMIT 1");

        AlertConfigVersionPO po = alertConfigVersionMapper.selectOne(query);
        return po == null ? null : po.getVersionNo();
    }

    @Override
    public void saveVersion(Long versionNo) {
        if (versionNo == null) {
            return;
        }
        AlertConfigVersionPO po = new AlertConfigVersionPO();
        po.setVersionNo(versionNo);
        po.setPublished(true);
        alertConfigVersionMapper.insert(po);
    }
}
