package com.stopper.asset.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stopper.asset.entity.StopperInventoryFreeze;
import com.stopper.asset.mapper.StopperInventoryFreezeMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StopperInventoryFreezeService extends ServiceImpl<StopperInventoryFreezeMapper, StopperInventoryFreeze> {

    public List<StopperInventoryFreeze> getByInventoryId(Long inventoryId) {
        return list(new LambdaQueryWrapper<StopperInventoryFreeze>()
                .eq(StopperInventoryFreeze::getInventoryId, inventoryId)
                .eq(StopperInventoryFreeze::getDeleted, 0)
                .orderByAsc(StopperInventoryFreeze::getStopperNo));
    }
}
