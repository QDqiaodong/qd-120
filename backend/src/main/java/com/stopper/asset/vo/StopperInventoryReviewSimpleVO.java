package com.stopper.asset.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StopperInventoryReviewSimpleVO {
    private Long id;
    private String reviewAction;
    private String reviewOperator;
    private LocalDateTime reviewTime;
    private String reviewRemark;
    private String originalStation;
    private String correctedStation;
    private String currentStation;
}
