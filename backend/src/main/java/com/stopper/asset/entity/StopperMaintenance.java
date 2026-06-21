package com.stopper.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("stopper_maintenance")
public class StopperMaintenance {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long stopperId;

    private String stopperNo;

    private String spec;

    private String adaptEquipment;

    private String originalStation;

    private String repairReason;

    private LocalDate expectedReturnDate;

    private LocalDateTime sendTime;

    private String sendOperator;

    private Integer status;

    private LocalDateTime completeTime;

    private String completeOperator;

    private String outcome;

    private String returnStation;

    private String completeRemark;

    private String remark;

    private LocalDateTime createTime;

    private Integer deleted;
}
