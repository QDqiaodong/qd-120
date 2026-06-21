package com.stopper.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stopper_inventory_freeze")
public class StopperInventoryFreeze {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long inventoryId;

    private Long stopperId;

    private String stopperNo;

    private String spec;

    private String station;

    private Integer status;

    private LocalDateTime freezeTime;

    private LocalDateTime createTime;

    private Integer deleted;
}
