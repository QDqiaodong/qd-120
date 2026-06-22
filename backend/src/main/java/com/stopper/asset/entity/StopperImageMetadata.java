package com.stopper.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stopper_image_metadata")
public class StopperImageMetadata {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String imageUrl;

    private String source;

    private Integer width;

    private Integer height;

    private String stopperNo;

    private LocalDateTime uploadTime;

    private String uploadOperator;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
