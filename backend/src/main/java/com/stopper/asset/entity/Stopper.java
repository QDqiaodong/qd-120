package com.stopper.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stopper")
public class Stopper {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String stopperNo;

    private String spec;

    private String adaptEquipment;

    private String station;

    private LocalDateTime storageTime;

    private String imageUrl;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
