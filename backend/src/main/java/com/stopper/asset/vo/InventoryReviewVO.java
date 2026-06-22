package com.stopper.asset.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class InventoryReviewVO {
    private Long detailId;
    private Long inventoryId;
    private String inventoryNo;
    private String inventoryMonth;
    private Long stopperId;
    private String stopperNo;
    private String spec;
    private String freezeStation;
    private String currentStation;
    private String diffReasonCode;
    private String diffReason;
    private Integer reviewStatus;
    private String reviewResult;
    private String reviewOperator;
    private LocalDateTime reviewTime;
    private String reviewRemark;
    private String correctedStation;
    private List<StopperInventoryReviewSimpleVO> reviewHistory;
}
