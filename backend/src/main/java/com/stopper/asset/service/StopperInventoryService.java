package com.stopper.asset.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stopper.asset.entity.Stopper;
import com.stopper.asset.entity.StopperInventory;
import com.stopper.asset.entity.StopperInventoryDetail;
import com.stopper.asset.entity.StopperInventoryFreeze;
import com.stopper.asset.mapper.StopperInventoryMapper;
import com.stopper.asset.vo.InventoryProgressVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class StopperInventoryService extends ServiceImpl<StopperInventoryMapper, StopperInventory> {

    @Autowired
    private StopperService stopperService;

    @Autowired
    private StopperInventoryDetailService detailService;

    @Autowired
    private StopperInventoryFreezeService freezeService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    public StopperInventory startInventory(String inventoryMonth, String operator) {
        if (inventoryMonth == null || inventoryMonth.trim().isEmpty()) {
            throw new RuntimeException("盘点月份不能为空");
        }
        if (operator == null || operator.trim().isEmpty()) {
            throw new RuntimeException("操作人不能为空");
        }

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        int maxRetries = 5;
        DuplicateKeyException lastException = null;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            final String inventoryNo = generateInventoryNo();
            try {
                return txTemplate.execute(status -> doStartInventory(inventoryMonth, operator, inventoryNo));
            } catch (DuplicateKeyException e) {
                lastException = e;
            }
        }
        throw new RuntimeException("盘点单号生成冲突，请稍后重试", lastException);
    }

    private String generateInventoryNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        int random = ThreadLocalRandom.current().nextInt(10000);
        return "INV" + timestamp + String.format("%04d", random);
    }

    private StopperInventory doStartInventory(String inventoryMonth, String operator, String inventoryNo) {
        Long existingProcessing = count(new LambdaQueryWrapper<StopperInventory>()
                .eq(StopperInventory::getInventoryMonth, inventoryMonth.trim())
                .eq(StopperInventory::getInventoryStatus, "PROCESSING")
                .eq(StopperInventory::getDeleted, 0));
        if (existingProcessing > 0) {
            throw new RuntimeException("该月份已有进行中的盘点单，请先完成或作废后再发起");
        }

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

        LocalDateTime freezeTime = LocalDateTime.now();
        for (Stopper stopper : allStoppers) {
            StopperInventoryDetail detail = new StopperInventoryDetail();
            detail.setInventoryId(inventory.getId());
            detail.setStopperId(stopper.getId());
            detail.setStopperNo(stopper.getStopperNo());
            detail.setSpec(stopper.getSpec());
            detail.setStation(stopper.getStation());
            detail.setInventoryStatus(0);
            detail.setReviewStatus(0);
            detail.setCreateTime(LocalDateTime.now());
            detail.setDeleted(0);
            detailService.save(detail);

            StopperInventoryFreeze freeze = new StopperInventoryFreeze();
            freeze.setInventoryId(inventory.getId());
            freeze.setStopperId(stopper.getId());
            freeze.setStopperNo(stopper.getStopperNo());
            freeze.setSpec(stopper.getSpec());
            freeze.setStation(stopper.getStation());
            freeze.setStatus(stopper.getStatus());
            freeze.setFreezeTime(freezeTime);
            freeze.setCreateTime(LocalDateTime.now());
            freeze.setDeleted(0);
            freezeService.save(freeze);
        }

        return inventory;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean markInventoryItem(Long detailId, Integer status, String diffReasonCode, String diffReason) {
        if (status == null || (status != 0 && status != 1 && status != 2)) {
            throw new RuntimeException("非法的盘点状态值，合法值为 0(未盘)、1(已盘)、2(差异)");
        }

        if (status != null && status == 2 && (diffReasonCode == null || diffReasonCode.trim().isEmpty())) {
            throw new RuntimeException("标记为差异时，标准原因不能为空");
        }

        StopperInventoryDetail detail = detailService.getById(detailId);
        if (detail == null) {
            throw new RuntimeException("盘点明细不存在");
        }

        StopperInventory inventory = getById(detail.getInventoryId());
        if (inventory == null) {
            throw new RuntimeException("盘点记录不存在");
        }
        if ("COMPLETED".equals(inventory.getInventoryStatus())) {
            throw new RuntimeException("该盘点单已完成，无法继续标记明细");
        }

        Stopper stopper = stopperService.getOne(new LambdaQueryWrapper<Stopper>()
                .eq(Stopper::getId, detail.getStopperId())
                .eq(Stopper::getDeleted, 0));
        if (stopper == null) {
            throw new RuntimeException("对应挡块已删除，无法继续盘点操作");
        }

        detail.setInventoryStatus(status);
        detail.setDiffReasonCode(diffReasonCode);
        detail.setDiffReason(diffReason);

        if (status != null && status == 2) {
            if ("MISSING".equals(diffReasonCode) || "MISPLACED".equals(diffReasonCode)) {
                detail.setReviewStatus(0);
            } else {
                detail.setReviewStatus(2);
            }
        } else if (status != null && status == 1) {
            detail.setReviewStatus(2);
        }

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
        if ("COMPLETED".equals(inventory.getInventoryStatus())) {
            throw new RuntimeException("该盘点单已完成，无法重复完成");
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
        long missingReasonCount = details.stream()
                .filter(detail -> detail.getInventoryStatus() != null
                        && detail.getInventoryStatus() == 2
                        && (detail.getDiffReasonCode() == null || detail.getDiffReasonCode().trim().isEmpty()))
                .count();
        if (missingReasonCount > 0) {
            throw new RuntimeException("还有 " + missingReasonCount + " 项差异未选择标准原因，请补充后再完成盘点");
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

    public List<StopperInventoryFreeze> getInventoryFreeze(Long inventoryId) {
        return freezeService.getByInventoryId(inventoryId);
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
