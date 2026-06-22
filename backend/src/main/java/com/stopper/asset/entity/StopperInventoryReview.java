package com.stopper.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stopper_inventory_review")
public class StopperInventoryReview {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long inventoryId;

    private Long detailId;

    private Long stopperId;

    private String stopperNo;

    private String spec;

    private String originalStation;

    private String currentStation;

    private String correctedStation;

    private String diffReasonCode;

    private String reviewAction;

    private String reviewOperator;

    private LocalDateTime reviewTime;

    private String reviewRemark;

    private LocalDateTime createTime;

    private Integer deleted;
}
