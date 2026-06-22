package com.stopper.asset.vo;

import lombok.Data;

@Data
public class InventoryReviewSummaryVO {
    private String inventoryMonth;
    private Long inventoryId;
    private String inventoryNo;
    private Integer totalDiffCount;
    private Integer pendingReviewCount;
    private Integer reviewedCount;
    private Integer correctStationCount;
    private Integer foundCount;
    private Integer scrapCount;
}
