package com.stopper.asset.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stopper.asset.entity.StopperInventoryReview;
import com.stopper.asset.mapper.StopperInventoryReviewMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StopperInventoryReviewService extends ServiceImpl<StopperInventoryReviewMapper, StopperInventoryReview> {

    public List<StopperInventoryReview> getByInventoryId(Long inventoryId) {
        return baseMapper.selectByInventoryId(inventoryId);
    }

    public List<StopperInventoryReview> getByDetailId(Long detailId) {
        return baseMapper.selectByDetailId(detailId);
    }

    public List<StopperInventoryReview> getByStopperId(Long stopperId) {
        return baseMapper.selectByStopperId(stopperId);
    }
}
