package com.stopper.asset.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stopper.asset.entity.Stopper;
import com.stopper.asset.entity.StopperInventory;
import com.stopper.asset.entity.StopperInventoryDetail;
import com.stopper.asset.mapper.StopperInventoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class StopperInventoryService extends ServiceImpl<StopperInventoryMapper, StopperInventory> {

    @Autowired
    private StopperService stopperService;

    @Autowired
    private StopperInventoryDetailService detailService;

    @Transactional(rollbackFor = Exception.class)
    public StopperInventory startInventory(String inventoryMonth, String operator) {
        String inventoryNo = "INV" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        StopperInventory inventory = new StopperInventory();
        inventory.setInventoryNo(inventoryNo);
        inventory.setInventoryMonth(inventoryMonth);
        inventory.setOperator(operator);
        inventory.setInventoryStatus("PROCESSING");
        inventory.setInventoryTime(LocalDateTime.now());
        inventory.setCreateTime(LocalDateTime.now());
        inventory.setDeleted(0);

        List<Stopper> allStoppers = stopperService.list(new LambdaQueryWrapper<Stopper>()
                .eq(Stopper::getStatus, 1)
                .eq(Stopper::getDeleted, 0));

        inventory.setTotalCount(allStoppers.size());
        inventory.setActualCount(0);
        inventory.setDiffCount(0);

        save(inventory);

        for (Stopper stopper : allStoppers) {
            StopperInventoryDetail detail = new StopperInventoryDetail();
            detail.setInventoryId(inventory.getId());
            detail.setStopperId(stopper.getId());
            detail.setStopperNo(stopper.getStopperNo());
            detail.setSpec(stopper.getSpec());
            detail.setStation(stopper.getStation());
            detail.setInventoryStatus(0);
            detail.setCreateTime(LocalDateTime.now());
            detail.setDeleted(0);
            detailService.save(detail);
        }

        return inventory;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean markInventoryItem(Long detailId, Integer status, String diffReason) {
        StopperInventoryDetail detail = detailService.getById(detailId);
        if (detail == null) {
            throw new RuntimeException("盘点明细不存在");
        }

        detail.setInventoryStatus(status);
        detail.setDiffReason(diffReason);
        detailService.updateById(detail);

        updateInventorySummary(detail.getInventoryId());
        return true;
    }

    private void updateInventorySummary(Long inventoryId) {
        List<StopperInventoryDetail> details = detailService.getByInventoryId(inventoryId);
        int actualCount = 0;
        int diffCount = 0;
        for (StopperInventoryDetail detail : details) {
            if (detail.getInventoryStatus() == 1) {
                actualCount++;
            } else {
                diffCount++;
            }
        }

        StopperInventory inventory = getById(inventoryId);
        inventory.setActualCount(actualCount);
        inventory.setDiffCount(diffCount);
        updateById(inventory);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean completeInventory(Long inventoryId, String remark) {
        StopperInventory inventory = getById(inventoryId);
        if (inventory == null) {
            throw new RuntimeException("盘点记录不存在");
        }
        inventory.setInventoryStatus("COMPLETED");
        inventory.setRemark(remark);
        inventory.setInventoryTime(LocalDateTime.now());
        return updateById(inventory);
    }

    public List<StopperInventory> getAllInventories() {
        return list(new LambdaQueryWrapper<StopperInventory>()
                .eq(StopperInventory::getDeleted, 0)
                .orderByDesc(StopperInventory::getInventoryTime));
    }

    public List<StopperInventoryDetail> getInventoryDetails(Long inventoryId) {
        return detailService.getByInventoryId(inventoryId);
    }
}
