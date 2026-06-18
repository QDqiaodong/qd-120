package com.stopper.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stopper_shift")
public class StopperShift {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long stopperId;

    private String stopperNo;

    private String fromStation;

    private String toStation;

    private String operator;

    private String shiftReason;

    private LocalDateTime shiftTime;

    private String remark;

    private LocalDateTime createTime;

    private Integer deleted;
}
