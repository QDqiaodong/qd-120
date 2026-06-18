package com.stopper.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stopper_inventory")
public class StopperInventory {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String inventoryNo;

    private String inventoryMonth;

    private Integer totalCount;

    private Integer actualCount;

    private Integer diffCount;

    private String inventoryStatus;

    private String operator;

    private LocalDateTime inventoryTime;

    private String remark;

    private LocalDateTime createTime;

    private Integer deleted;
}
