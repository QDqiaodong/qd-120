package com.stopper.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stopper_equipment_change")
public class StopperEquipmentChange {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long stopperId;

    private String stopperNo;

    private String spec;

    private String oldEquipment;

    private String newEquipment;

    private String changeReason;

    private String operator;

    private LocalDateTime changeTime;

    private String remark;

    private LocalDateTime createTime;

    private Integer deleted;
}
