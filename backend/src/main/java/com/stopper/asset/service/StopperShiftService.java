package com.stopper.asset.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stopper.asset.entity.Stopper;
import com.stopper.asset.entity.StopperShift;
import com.stopper.asset.exception.ValidationException;
import com.stopper.asset.mapper.StopperShiftMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StopperShiftService extends ServiceImpl<StopperShiftMapper, StopperShift> {

    @Autowired
    private StopperService stopperService;

    private static final Integer STATUS_SCRAPPED = 2;

    @Transactional(rollbackFor = Exception.class)
    public Stopper addShift(StopperShift shift) {
        Stopper stopper = stopperService.getOne(new LambdaQueryWrapper<Stopper>()
                .eq(Stopper::getId, shift.getStopperId())
                .eq(Stopper::getDeleted, 0));
        if (stopper == null) {
            throw new RuntimeException("挡块不存在或已删除");
        }

        Map<String, String> fieldErrors = new HashMap<>();

        if (STATUS_SCRAPPED.equals(stopper.getStatus())) {
            fieldErrors.put("stopperId", "报废挡块不可移位");
        }

        String fromStation = stopper.getStation();
        String toStation = shift.getToStation();

        if (toStation != null && toStation.trim().equals(fromStation != null ? fromStation.trim() : "")) {
            fieldErrors.put("toStation", "同工位不可重复登记");
        }

        if (fromStation != null && fromStation.contains("维修")) {
            String shiftReason = shift.getShiftReason();
            if (shiftReason == null || shiftReason.trim().isEmpty()) {
                fieldErrors.put("shiftReason", "维修区返回需填写原因");
            }
        }

        if (!fieldErrors.isEmpty()) {
            throw new ValidationException("工位变更校验失败", fieldErrors);
        }

        shift.setFromStation(fromStation);
        shift.setStopperNo(stopper.getStopperNo());
        shift.setSpec(stopper.getSpec());
        shift.setAdaptEquipment(stopper.getAdaptEquipment());
        shift.setShiftTime(LocalDateTime.now());
        shift.setCreateTime(LocalDateTime.now());
        shift.setDeleted(0);

        boolean shiftResult = save(shift);
        if (!shiftResult) {
            return null;
        }

        boolean updateResult = stopperService.updateStation(shift.getStopperId(), shift.getToStation());
        if (!updateResult) {
            return null;
        }

        return stopperService.getById(shift.getStopperId());
    }

    public List<StopperShift> getShiftList(Long stopperId) {
        return getBaseMapper().selectList(new LambdaQueryWrapper<StopperShift>()
                .eq(StopperShift::getDeleted, 0)
                .eq(stopperId != null, StopperShift::getStopperId, stopperId)
                .orderByDesc(StopperShift::getShiftTime));
    }

    public List<StopperShift> getAllShifts() {
        return list(new LambdaQueryWrapper<StopperShift>()
                .eq(StopperShift::getDeleted, 0)
                .orderByDesc(StopperShift::getShiftTime));
    }
}
