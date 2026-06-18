package com.stopper.asset.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stopper.asset.entity.Stopper;
import com.stopper.asset.entity.StopperShift;
import com.stopper.asset.mapper.StopperShiftMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StopperShiftService extends ServiceImpl<StopperShiftMapper, StopperShift> {

    @Autowired
    private StopperService stopperService;

    @Transactional(rollbackFor = Exception.class)
    public boolean addShift(StopperShift shift) {
        Stopper stopper = stopperService.getById(shift.getStopperId());
        if (stopper == null) {
            throw new RuntimeException("挡块不存在");
        }

        shift.setFromStation(stopper.getStation());
        shift.setStopperNo(stopper.getStopperNo());
        shift.setShiftTime(LocalDateTime.now());
        shift.setCreateTime(LocalDateTime.now());
        shift.setDeleted(0);

        boolean shiftResult = save(shift);
        if (!shiftResult) {
            return false;
        }

        return stopperService.updateStation(shift.getStopperId(), shift.getToStation());
    }

    public List<StopperShift> getShiftList(Long stopperId) {
        LambdaQueryWrapper<StopperShift> wrapper = new LambdaQueryWrapper<>();
        if (stopperId != null) {
            wrapper.eq(StopperShift::getStopperId, stopperId);
        }
        wrapper.eq(StopperShift::getDeleted, 0)
                .orderByDesc(StopperShift::getShiftTime);
        return list(wrapper);
    }

    public List<StopperShift> getAllShifts() {
        return list(new LambdaQueryWrapper<StopperShift>()
                .eq(StopperShift::getDeleted, 0)
                .orderByDesc(StopperShift::getShiftTime));
    }
}
