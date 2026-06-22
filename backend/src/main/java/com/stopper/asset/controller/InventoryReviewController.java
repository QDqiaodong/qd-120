package com.stopper.asset.controller;

import com.stopper.asset.common.Result;
import com.stopper.asset.entity.Stopper;
import com.stopper.asset.entity.StopperInventoryReview;
import com.stopper.asset.service.InventoryReviewService;
import com.stopper.asset.service.StopperInventoryReviewService;
import com.stopper.asset.vo.InventoryReviewSummaryVO;
import com.stopper.asset.vo.InventoryReviewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory-review")
public class InventoryReviewController {

    @Autowired
    private InventoryReviewService inventoryReviewService;

    @Autowired
    private StopperInventoryReviewService reviewHistoryService;

    @GetMapping("/summary")
    public Result<List<InventoryReviewSummaryVO>> getSummary() {
        return Result.success(inventoryReviewService.getReviewSummary());
    }

    @GetMapping("/pending")
    public Result<List<InventoryReviewVO>> getPendingReviews(
            @RequestParam(required = false) Long inventoryId) {
        return Result.success(inventoryReviewService.getPendingReviews(inventoryId));
    }

    @GetMapping("/reviewed")
    public Result<List<InventoryReviewVO>> getReviewedList(
            @RequestParam(required = false) Long inventoryId) {
        return Result.success(inventoryReviewService.getReviewedList(inventoryId));
    }

    @GetMapping("/history/{detailId}")
    public Result<List<StopperInventoryReview>> getHistory(@PathVariable Long detailId) {
        return Result.success(reviewHistoryService.getByDetailId(detailId));
    }

    @PostMapping("/second-confirm")
    public Result<String> secondConfirm(@RequestBody Map<String, Object> params) {
        Long detailId = Long.valueOf(params.get("detailId").toString());
        String operator = params.get("operator") != null ? params.get("operator").toString() : "系统";
        String remark = params.get("remark") != null ? params.get("remark").toString() : null;
        boolean result = inventoryReviewService.secondConfirm(detailId, operator, remark);
        return result ? Result.success("二次确认完成") : Result.error("操作失败");
    }

    @PostMapping("/correct-station")
    public Result<String> correctStation(@RequestBody Map<String, Object> params) {
        Long detailId = Long.valueOf(params.get("detailId").toString());
        String correctedStation = params.get("correctedStation") != null ? params.get("correctedStation").toString() : null;
        String operator = params.get("operator") != null ? params.get("operator").toString() : "系统";
        String remark = params.get("remark") != null ? params.get("remark").toString() : null;
        boolean result = inventoryReviewService.correctStation(detailId, correctedStation, operator, remark);
        return result ? Result.success("工位修正成功") : Result.error("操作失败");
    }

    @PostMapping("/mark-found")
    public Result<String> markFound(@RequestBody Map<String, Object> params) {
        Long detailId = Long.valueOf(params.get("detailId").toString());
        String foundStation = params.get("foundStation") != null ? params.get("foundStation").toString() : null;
        String operator = params.get("operator") != null ? params.get("operator").toString() : "系统";
        String remark = params.get("remark") != null ? params.get("remark").toString() : null;
        boolean result = inventoryReviewService.markFound(detailId, foundStation, operator, remark);
        return result ? Result.success("标记找回成功") : Result.error("操作失败");
    }

    @PostMapping("/process-scrap")
    public Result<Stopper> processScrap(@RequestBody Map<String, Object> params) {
        Long detailId = Long.valueOf(params.get("detailId").toString());
        String scrapReason = params.get("scrapReason") != null ? params.get("scrapReason").toString() : null;
        String scrapDegree = params.get("scrapDegree") != null ? params.get("scrapDegree").toString() : null;
        String operator = params.get("operator") != null ? params.get("operator").toString() : "系统";
        String remark = params.get("remark") != null ? params.get("remark").toString() : null;
        Stopper stopper = inventoryReviewService.processScrap(detailId, scrapReason, scrapDegree, operator, remark);
        return stopper != null ? Result.success(stopper) : Result.error("操作失败");
    }
}
