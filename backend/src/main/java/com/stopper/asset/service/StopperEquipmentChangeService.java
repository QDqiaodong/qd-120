package com.stopper.asset.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stopper.asset.entity.Stopper;
import com.stopper.asset.entity.StopperEquipmentChange;
import com.stopper.asset.mapper.StopperEquipmentChangeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StopperEquipmentChangeService extends ServiceImpl<StopperEquipmentChangeMapper, StopperEquipmentChange> {

    public List<StopperEquipmentChange> getByStopperId(Long stopperId) {
        return list(new LambdaQueryWrapper<StopperEquipmentChange>()
                .eq(StopperEquipmentChange::getStopperId, stopperId)
                .eq(StopperEquipmentChange::getDeleted, 0)
                .orderByDesc(StopperEquipmentChange::getChangeTime));
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean recordEquipmentChange(Stopper stopper, String oldEquipment, String newEquipment, String changeReason, String operator) {
        if (stopper == null || stopper.getId() == null) {
            return false;
        }
        StopperEquipmentChange change = new StopperEquipmentChange();
        change.setStopperId(stopper.getId());
        change.setStopperNo(stopper.getStopperNo());
        change.setSpec(stopper.getSpec());
        change.setOldEquipment(oldEquipment);
        change.setNewEquipment(newEquipment);
        change.setChangeReason(changeReason);
        change.setOperator(operator);
        change.setChangeTime(LocalDateTime.now());
        change.setCreateTime(LocalDateTime.now());
        change.setDeleted(0);
        return save(change);
    }

    public boolean hasEquipmentChange(Long stopperId, String oldEquipment, String newEquipment) {
        String oldEq = oldEquipment == null ? "" : oldEquipment.trim();
        String newEq = newEquipment == null ? "" : newEquipment.trim();
        return !oldEq.equals(newEq);
    }
}
