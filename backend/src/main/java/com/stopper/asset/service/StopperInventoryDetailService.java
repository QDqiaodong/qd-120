package com.stopper.asset.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stopper.asset.entity.StopperInventoryDetail;
import com.stopper.asset.mapper.StopperInventoryDetailMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StopperInventoryDetailService extends ServiceImpl<StopperInventoryDetailMapper, StopperInventoryDetail> {

    public List<StopperInventoryDetail> getByInventoryId(Long inventoryId) {
        return baseMapper.selectByInventoryId(inventoryId);
    }
}
