package com.stopper.asset.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StopperEquipmentVO {
    private Long id;

    private String stopperNo;

    private String spec;

    private String adaptEquipment;

    private String station;

    private LocalDateTime lastShiftTime;
}
