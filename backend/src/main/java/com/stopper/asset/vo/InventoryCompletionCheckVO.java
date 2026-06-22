package com.stopper.asset.vo;

import lombok.Data;
import java.util.List;

@Data
public class InventoryCompletionCheckVO {
    private Boolean isAllProcessed;
    private Integer unprocessedCount;
    private List<String> unprocessedStopperNos;
}
