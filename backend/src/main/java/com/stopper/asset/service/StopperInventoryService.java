package com.stopper.asset.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stopper.asset.entity.Stopper;
import com.stopper.asset.entity.StopperInventory;
import com.stopper.asset.entity.StopperInventoryDetail;
import com.stopper.asset.mapper.StopperInventoryMapper;
import com.stopper.asset.vo.InventoryProgressVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StopperInventoryService extends ServiceImpl<StopperInventoryMapper, StopperInventory> {

    @Autowired
    private StopperService stopperService;

    @Autowired
    private StopperInventoryDetailService detailService;

    @Transactional(rollbackFor = Exception.class)
    public StopperInventory startInventory(String inventoryMonth, String operator) {
        if (inventoryMonth == null || inventoryMonth.trim().isEmpty()) {
            throw new RuntimeException("盘点月份不能为空");
        }
        if (operator == null || operator.trim().isEmpty()) {
            throw new RuntimeException("操作人不能为空");
        }

        Long existingProcessing = count(new LambdaQueryWrapper<StopperInventory>()
                .eq(StopperInventory::getInventoryMonth, inventoryMonth.trim())
                .eq(StopperInventory::getInventoryStatus, "PROCESSING")
                .eq(StopperInventory::getDeleted, 0));
        if (existingProcessing > 0) {
            throw new RuntimeException("该月份已有进行中的盘点单，请先完成或作废后再发起");
        }

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
        if (status == null || (status != 0 && status != 1 && status != 2)) {
            throw new RuntimeException("非法的盘点状态值，合法值为 0(未盘)、1(已盘)、2(差异)");
        }

        StopperInventoryDetail detail = detailService.getById(detailId);
        if (detail == null) {
            throw new RuntimeException("盘点明细不存在");
        }

        Stopper stopper = stopperService.getOne(new LambdaQueryWrapper<Stopper>()
                .eq(Stopper::getId, detail.getStopperId())
                .eq(Stopper::getDeleted, 0));
        if (stopper == null) {
            throw new RuntimeException("对应挡块已删除，无法继续盘点操作");
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
            Integer status = detail.getInventoryStatus();
            if (status != null && status == 1) {
                actualCount++;
            } else if (status != null && status == 2) {
                diffCount++;
            }
        }

        StopperInventory inventory = getById(inventoryId);
        inventory.setTotalCount(details.size());
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
        List<StopperInventoryDetail> details = detailService.getByInventoryId(inventoryId);
        long pendingCount = details.stream()
                .filter(detail -> {
                    Integer status = detail.getInventoryStatus();
                    return status == null || status == 0 || (status != 1 && status != 2);
                })
                .count();
        if (pendingCount > 0) {
            throw new RuntimeException("还有 " + pendingCount + " 项挡块未盘点，请全部处理后再完成盘点");
        }
        updateInventorySummary(inventoryId);
        inventory = getById(inventoryId);
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

    public InventoryProgressVO getCurrentMonthProgress() {
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        
        StopperInventory inventory = getOne(new LambdaQueryWrapper<StopperInventory>()
                .eq(StopperInventory::getInventoryMonth, currentMonth)
                .eq(StopperInventory::getDeleted, 0)
                .orderByDesc(StopperInventory::getCreateTime)
                .last("LIMIT 1"));
        
        if (inventory == null) {
            InventoryProgressVO vo = new InventoryProgressVO();
            vo.setInventoryMonth(currentMonth);
            vo.setInventoryStatus("NOT_STARTED");
            vo.setTotalCount(0);
            vo.setCountedCount(0);
            vo.setDiscrepancyCount(0);
            vo.setUnprocessedCount(0);
            vo.setCompletionRatio(0.0);
            return vo;
        }
        
        List<StopperInventoryDetail> details = detailService.getByInventoryId(inventory.getId());
        
        List<StopperInventoryDetail> countedList = details.stream()
                .filter(d -> {
                    Integer status = d.getInventoryStatus();
                    return status != null && status == 1;
                })
                .collect(Collectors.toList());
        List<StopperInventoryDetail> discrepancyList = details.stream()
                .filter(d -> {
                    Integer status = d.getInventoryStatus();
                    return status != null && status == 2;
                })
                .collect(Collectors.toList());
        List<StopperInventoryDetail> unprocessedList = details.stream()
                .filter(d -> {
                    Integer status = d.getInventoryStatus();
                    return status == null || status == 0 || (status != 1 && status != 2);
                })
                .collect(Collectors.toList());
        
        int total = details.size();
        int counted = countedList.size();
        int discrepancy = discrepancyList.size();
        int unprocessed = unprocessedList.size();
        double ratio = total > 0 ? (double) (counted + discrepancy) / total * 100 : 0;
        
        InventoryProgressVO vo = new InventoryProgressVO();
        vo.setInventoryId(inventory.getId());
        vo.setInventoryNo(inventory.getInventoryNo());
        vo.setInventoryMonth(inventory.getInventoryMonth());
        vo.setInventoryStatus(inventory.getInventoryStatus());
        vo.setTotalCount(total);
        vo.setCountedCount(counted);
        vo.setDiscrepancyCount(discrepancy);
        vo.setUnprocessedCount(unprocessed);
        vo.setCompletionRatio(Math.round(ratio * 100.0) / 100.0);
        vo.setCountedList(countedList);
        vo.setDiscrepancyList(discrepancyList);
        vo.setUnprocessedList(unprocessedList);
        
        return vo;
    }
}
