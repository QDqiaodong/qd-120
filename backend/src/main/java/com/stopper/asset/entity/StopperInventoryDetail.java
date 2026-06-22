package com.stopper.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stopper_inventory_detail")
public class StopperInventoryDetail {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long inventoryId;

    private Long stopperId;

    private String stopperNo;

    private String spec;

    private String station;

    private Integer inventoryStatus;

    private String diffReasonCode;

    private String diffReason;

    private LocalDateTime createTime;

    private Integer deleted;
}
