package com.stopper.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stopper_scrap")
public class StopperScrap {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long stopperId;

    private String stopperNo;

    private String spec;

    private String scrapReason;

    private String scrapDegree;

    private String operator;

    private LocalDateTime scrapTime;

    private String remark;

    private LocalDateTime createTime;

    private Integer deleted;
}
