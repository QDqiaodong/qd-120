package com.stopper.asset.controller;

import com.stopper.asset.common.Result;
import com.stopper.asset.entity.StopperInventory;
import com.stopper.asset.entity.StopperInventoryDetail;
import com.stopper.asset.entity.StopperInventoryFreeze;
import com.stopper.asset.service.StopperInventoryService;
import com.stopper.asset.vo.InventoryProgressVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class StopperInventoryController {

    @Autowired
    private StopperInventoryService inventoryService;

    @GetMapping("/list")
    public Result<List<StopperInventory>> list() {
        return Result.success(inventoryService.getAllInventories());
    }

    @GetMapping("/{id}")
    public Result<StopperInventory> getById(@PathVariable Long id) {
        return Result.success(inventoryService.getById(id));
    }

    @GetMapping("/detail/{inventoryId}")
    public Result<List<StopperInventoryDetail>> getDetails(@PathVariable Long inventoryId) {
        return Result.success(inventoryService.getInventoryDetails(inventoryId));
    }

    @GetMapping("/freeze/{inventoryId}")
    public Result<List<StopperInventoryFreeze>> getFreeze(@PathVariable Long inventoryId) {
        return Result.success(inventoryService.getInventoryFreeze(inventoryId));
    }

    @PostMapping("/start")
    public Result<StopperInventory> start(@RequestBody Map<String, String> params) {
        String inventoryMonth = params.get("inventoryMonth");
        String operator = params.get("operator");
        StopperInventory inventory = inventoryService.startInventory(inventoryMonth, operator);
        return Result.success(inventory);
    }

    @PostMapping("/mark")
    public Result<String> markItem(@RequestBody Map<String, Object> params) {
        Long detailId = Long.valueOf(params.get("detailId").toString());
        Integer status = Integer.valueOf(params.get("status").toString());
        String diffReasonCode = params.get("diffReasonCode") != null ? params.get("diffReasonCode").toString() : null;
        String diffReason = params.get("diffReason") != null ? params.get("diffReason").toString() : null;
        boolean result = inventoryService.markInventoryItem(detailId, status, diffReasonCode, diffReason);
        return result ? Result.success("标记成功") : Result.error("标记失败");
    }

    @PostMapping("/complete")
    public Result<String> complete(@RequestBody Map<String, Object> params) {
        Long inventoryId = Long.valueOf(params.get("inventoryId").toString());
        String remark = params.get("remark") != null ? params.get("remark").toString() : null;
        boolean result = inventoryService.completeInventory(inventoryId, remark);
        return result ? Result.success("盘点完成") : Result.error("操作失败");
    }

    @GetMapping("/progress")
    public Result<InventoryProgressVO> getCurrentMonthProgress() {
        return Result.success(inventoryService.getCurrentMonthProgress());
    }
}
