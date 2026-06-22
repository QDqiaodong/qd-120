package com.stopper.asset.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stopper.asset.entity.*;
import com.stopper.asset.vo.InventoryReviewSummaryVO;
import com.stopper.asset.vo.InventoryReviewVO;
import com.stopper.asset.vo.StopperInventoryReviewSimpleVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InventoryReviewService {

    @Autowired
    private StopperInventoryService inventoryService;

    @Autowired
    private StopperInventoryDetailService detailService;

    @Autowired
    private StopperInventoryFreezeService freezeService;

    @Autowired
    private StopperInventoryReviewService reviewService;

    @Autowired
    private StopperService stopperService;

    @Autowired
    private StopperShiftService shiftService;

    @Autowired
    private StopperScrapService scrapService;

    private static final String DIFF_MISSING = "MISSING";
    private static final String DIFF_MISPLACED = "MISPLACED";
    private static final String ACTION_SECOND_CONFIRM = "SECOND_CONFIRM";
    private static final String ACTION_CORRECT_STATION = "CORRECT_STATION";
    private static final String ACTION_FOUND = "FOUND";
    private static final String ACTION_SCRAP = "SCRAP";
    private static final String RESULT_CORRECT_STATION = "CORRECT_STATION";
    private static final String RESULT_FOUND = "FOUND";
    private static final String RESULT_SCRAP = "SCRAP";

    public List<InventoryReviewVO> getPendingReviews(Long inventoryId) {
        LambdaQueryWrapper<StopperInventoryDetail> wrapper = new LambdaQueryWrapper<>();
        if (inventoryId != null) {
            wrapper.eq(StopperInventoryDetail::getInventoryId, inventoryId);
        }
        wrapper.eq(StopperInventoryDetail::getInventoryStatus, 2)
                .in(StopperInventoryDetail::getDiffReasonCode, DIFF_MISSING, DIFF_MISPLACED)
                .eq(StopperInventoryDetail::getDeleted, 0)
                .ne(StopperInventoryDetail::getReviewStatus, 1);

        List<StopperInventoryDetail> details = detailService.list(wrapper);
        return buildReviewVOList(details);
    }

    public List<InventoryReviewVO> getReviewedList(Long inventoryId) {
        LambdaQueryWrapper<StopperInventoryDetail> wrapper = new LambdaQueryWrapper<>();
        if (inventoryId != null) {
            wrapper.eq(StopperInventoryDetail::getInventoryId, inventoryId);
        }
        wrapper.eq(StopperInventoryDetail::getInventoryStatus, 2)
                .in(StopperInventoryDetail::getDiffReasonCode, DIFF_MISSING, DIFF_MISPLACED)
                .eq(StopperInventoryDetail::getReviewStatus, 1)
                .eq(StopperInventoryDetail::getDeleted, 0);

        List<StopperInventoryDetail> details = detailService.list(wrapper);
        return buildReviewVOList(details);
    }

    public List<InventoryReviewSummaryVO> getReviewSummary() {
        List<StopperInventory> inventories = inventoryService.getAllInventories();
        List<InventoryReviewSummaryVO> result = new ArrayList<>();

        for (StopperInventory inv : inventories) {
            List<StopperInventoryDetail> diffList = detailService.list(
                    new LambdaQueryWrapper<StopperInventoryDetail>()
                            .eq(StopperInventoryDetail::getInventoryId, inv.getId())
                            .eq(StopperInventoryDetail::getInventoryStatus, 2)
                            .in(StopperInventoryDetail::getDiffReasonCode, DIFF_MISSING, DIFF_MISPLACED)
                            .eq(StopperInventoryDetail::getDeleted, 0)
            );

            if (diffList.isEmpty()) {
                continue;
            }

            InventoryReviewSummaryVO vo = new InventoryReviewSummaryVO();
            vo.setInventoryId(inv.getId());
            vo.setInventoryNo(inv.getInventoryNo());
            vo.setInventoryMonth(inv.getInventoryMonth());
            vo.setTotalDiffCount(diffList.size());

            int pending = 0, reviewed = 0, correct = 0, found = 0, scrap = 0;
            for (StopperInventoryDetail d : diffList) {
                if (d.getReviewStatus() != null && d.getReviewStatus() == 1) {
                    reviewed++;
                    if (RESULT_CORRECT_STATION.equals(d.getReviewResult())) correct++;
                    else if (RESULT_FOUND.equals(d.getReviewResult())) found++;
                    else if (RESULT_SCRAP.equals(d.getReviewResult())) scrap++;
                } else {
                    pending++;
                }
            }
            vo.setPendingReviewCount(pending);
            vo.setReviewedCount(reviewed);
            vo.setCorrectStationCount(correct);
            vo.setFoundCount(found);
            vo.setScrapCount(scrap);
            result.add(vo);
        }
        return result;
    }

    private List<InventoryReviewVO> buildReviewVOList(List<StopperInventoryDetail> details) {
        if (details.isEmpty()) return new ArrayList<>();

        List<Long> inventoryIds = details.stream().map(StopperInventoryDetail::getInventoryId).distinct().collect(Collectors.toList());
        List<Long> stopperIds = details.stream().map(StopperInventoryDetail::getStopperId).distinct().collect(Collectors.toList());
        List<Long> detailIds = details.stream().map(StopperInventoryDetail::getId).collect(Collectors.toList());

        Map<Long, StopperInventory> inventoryMap = inventoryService.listByIds(inventoryIds).stream()
                .collect(Collectors.toMap(StopperInventory::getId, i -> i));
        Map<Long, StopperInventoryFreeze> freezeMap = freezeService.list(
                new LambdaQueryWrapper<StopperInventoryFreeze>()
                        .in(StopperInventoryFreeze::getInventoryId, inventoryIds)
                        .in(StopperInventoryFreeze::getStopperId, stopperIds)
                        .eq(StopperInventoryFreeze::getDeleted, 0)
        ).stream().collect(Collectors.toMap(
                f -> f.getInventoryId() * 100000 + f.getStopperId(),
                f -> f,
                (a, b) -> a
        ));
        Map<Long, List<StopperInventoryReview>> reviewMap = reviewService.list(
                new LambdaQueryWrapper<StopperInventoryReview>()
                        .in(StopperInventoryReview::getDetailId, detailIds)
                        .eq(StopperInventoryReview::getDeleted, 0)
                        .orderByDesc(StopperInventoryReview::getReviewTime)
        ).stream().collect(Collectors.groupingBy(StopperInventoryReview::getDetailId));

        List<InventoryReviewVO> result = new ArrayList<>();
        for (StopperInventoryDetail detail : details) {
            InventoryReviewVO vo = new InventoryReviewVO();
            vo.setDetailId(detail.getId());
            vo.setInventoryId(detail.getInventoryId());
            StopperInventory inv = inventoryMap.get(detail.getInventoryId());
            if (inv != null) {
                vo.setInventoryNo(inv.getInventoryNo());
                vo.setInventoryMonth(inv.getInventoryMonth());
            }
            vo.setStopperId(detail.getStopperId());
            vo.setStopperNo(detail.getStopperNo());
            vo.setSpec(detail.getSpec());
            vo.setCurrentStation(detail.getStation());
            vo.setDiffReasonCode(detail.getDiffReasonCode());
            vo.setDiffReason(detail.getDiffReason());
            vo.setReviewStatus(detail.getReviewStatus());
            vo.setReviewResult(detail.getReviewResult());
            vo.setReviewOperator(detail.getReviewOperator());
            vo.setReviewTime(detail.getReviewTime());
            vo.setReviewRemark(detail.getReviewRemark());
            vo.setCorrectedStation(detail.getCorrectedStation());

            StopperInventoryFreeze freeze = freezeMap.get(detail.getInventoryId() * 100000 + detail.getStopperId());
            if (freeze != null) {
                vo.setFreezeStation(freeze.getStation());
            }

            List<StopperInventoryReview> reviews = reviewMap.get(detail.getId());
            if (reviews != null) {
                List<StopperInventoryReviewSimpleVO> history = new ArrayList<>();
                for (StopperInventoryReview r : reviews) {
                    StopperInventoryReviewSimpleVO s = new StopperInventoryReviewSimpleVO();
                    s.setId(r.getId());
                    s.setReviewAction(r.getReviewAction());
                    s.setReviewOperator(r.getReviewOperator());
                    s.setReviewTime(r.getReviewTime());
                    s.setReviewRemark(r.getReviewRemark());
                    s.setOriginalStation(r.getOriginalStation());
                    s.setCorrectedStation(r.getCorrectedStation());
                    s.setCurrentStation(r.getCurrentStation());
                    history.add(s);
                }
                vo.setReviewHistory(history);
            }
            result.add(vo);
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean secondConfirm(Long detailId, String operator, String remark) {
        StopperInventoryDetail detail = detailService.getById(detailId);
        validateDetailForReview(detail);

        StopperInventoryReview review = buildReviewRecord(detail, ACTION_SECOND_CONFIRM, operator, remark);
        reviewService.save(review);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean correctStation(Long detailId, String correctedStation, String operator, String remark) {
        if (correctedStation == null || correctedStation.trim().isEmpty()) {
            throw new RuntimeException("修正后工位不能为空");
        }
        StopperInventoryDetail detail = detailService.getById(detailId);
        validateDetailForReview(detail);

        Stopper stopper = stopperService.getById(detail.getStopperId());
        if (stopper == null || stopper.getDeleted() == 1) {
            throw new RuntimeException("挡块不存在或已删除");
        }
        if (stopper.getStatus() != null && stopper.getStatus() == 2) {
            throw new RuntimeException("挡块已报废，无法修正工位");
        }

        StopperShift shift = new StopperShift();
        shift.setStopperId(stopper.getId());
        shift.setToStation(correctedStation.trim());
        shift.setOperator(operator);
        shift.setShiftReason("盘点差异复核-工位修正：" + (remark != null ? remark : ""));
        shiftService.addShift(shift);

        detail.setCorrectedStation(correctedStation.trim());

        StopperInventoryReview review = buildReviewRecord(detail, ACTION_CORRECT_STATION, operator, remark);
        review.setCorrectedStation(correctedStation.trim());
        reviewService.save(review);

        markDetailReviewed(detail, RESULT_CORRECT_STATION, operator, remark);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean markFound(Long detailId, String foundStation, String operator, String remark) {
        StopperInventoryDetail detail = detailService.getById(detailId);
        validateDetailForReview(detail);

        Stopper stopper = stopperService.getById(detail.getStopperId());
        if (stopper == null || stopper.getDeleted() == 1) {
            throw new RuntimeException("挡块不存在或已删除");
        }
        if (stopper.getStatus() != null && stopper.getStatus() == 2) {
            throw new RuntimeException("挡块已报废，无法标记找回");
        }

        if (foundStation != null && !foundStation.trim().isEmpty()) {
            StopperShift shift = new StopperShift();
            shift.setStopperId(stopper.getId());
            shift.setToStation(foundStation.trim());
            shift.setOperator(operator);
            shift.setShiftReason("盘点差异复核-挡块找回：" + (remark != null ? remark : ""));
            shiftService.addShift(shift);
            detail.setCorrectedStation(foundStation.trim());
        }

        StopperInventoryReview review = buildReviewRecord(detail, ACTION_FOUND, operator, remark);
        if (foundStation != null && !foundStation.trim().isEmpty()) {
            review.setCorrectedStation(foundStation.trim());
        }
        reviewService.save(review);

        markDetailReviewed(detail, RESULT_FOUND, operator, remark);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public Stopper processScrap(Long detailId, String scrapReason, String scrapDegree, String operator, String remark) {
        StopperInventoryDetail detail = detailService.getById(detailId);
        validateDetailForReview(detail);

        Stopper stopper = stopperService.getById(detail.getStopperId());
        if (stopper == null || stopper.getDeleted() == 1) {
            throw new RuntimeException("挡块不存在或已删除");
        }
        if (stopper.getStatus() != null && stopper.getStatus() == 2) {
            throw new RuntimeException("挡块已报废，不可重复报废");
        }

        StopperScrap scrap = new StopperScrap();
        scrap.setStopperId(stopper.getId());
        if (scrapReason != null && !scrapReason.trim().isEmpty()) {
            scrap.setScrapReason(scrapReason.trim());
        } else {
            scrap.setScrapReason("盘点差异复核报废：" + (DIFF_MISSING.equals(detail.getDiffReasonCode()) ? "缺失" : "错位"));
        }
        scrap.setScrapDegree(scrapDegree);
        scrap.setOperator(operator);
        scrap.setRemark(remark);
        Stopper updatedStopper = scrapService.addScrap(scrap);
        if (updatedStopper == null) {
            throw new RuntimeException("报废登记失败");
        }

        StopperInventoryReview review = buildReviewRecord(detail, ACTION_SCRAP, operator, remark);
        reviewService.save(review);

        markDetailReviewed(detail, RESULT_SCRAP, operator, remark);
        return updatedStopper;
    }

    private void validateDetailForReview(StopperInventoryDetail detail) {
        if (detail == null) {
            throw new RuntimeException("盘点明细不存在");
        }
        if (detail.getInventoryStatus() == null || detail.getInventoryStatus() != 2) {
            throw new RuntimeException("该明细不是差异状态，无需复核");
        }
        if (!DIFF_MISSING.equals(detail.getDiffReasonCode()) && !DIFF_MISPLACED.equals(detail.getDiffReasonCode())) {
            throw new RuntimeException("仅缺失或错位差异需要复核");
        }
        if (detail.getReviewStatus() != null && detail.getReviewStatus() == 1) {
            throw new RuntimeException("该明细已复核完成");
        }
    }

    private StopperInventoryReview buildReviewRecord(StopperInventoryDetail detail, String action, String operator, String remark) {
        StopperInventoryReview review = new StopperInventoryReview();
        review.setInventoryId(detail.getInventoryId());
        review.setDetailId(detail.getId());
        review.setStopperId(detail.getStopperId());
        review.setStopperNo(detail.getStopperNo());
        review.setSpec(detail.getSpec());
        review.setCurrentStation(detail.getStation());
        review.setDiffReasonCode(detail.getDiffReasonCode());
        review.setReviewAction(action);
        review.setReviewOperator(operator);
        review.setReviewTime(LocalDateTime.now());
        review.setReviewRemark(remark);
        review.setCreateTime(LocalDateTime.now());
        review.setDeleted(0);
        return review;
    }

    private void markDetailReviewed(StopperInventoryDetail detail, String result, String operator, String remark) {
        detail.setReviewStatus(1);
        detail.setReviewResult(result);
        detail.setReviewOperator(operator);
        detail.setReviewTime(LocalDateTime.now());
        detail.setReviewRemark(remark);
        detailService.updateById(detail);
    }
}
