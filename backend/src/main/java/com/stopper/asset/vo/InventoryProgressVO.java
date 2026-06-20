package com.stopper.asset.vo;

import com.stopper.asset.entity.StopperInventoryDetail;
import lombok.Data;

import java.util.List;

@Data
public class InventoryProgressVO {
    private Long inventoryId;
    private String inventoryNo;
    private String inventoryMonth;
    private String inventoryStatus;
    private Integer totalCount;
    private Integer countedCount;
    private Integer discrepancyCount;
    private Integer unprocessedCount;
    private Double completionRatio;
    private List<StopperInventoryDetail> countedList;
    private List<StopperInventoryDetail> discrepancyList;
    private List<StopperInventoryDetail> unprocessedList;
}
