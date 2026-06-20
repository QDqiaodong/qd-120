package com.stopper.asset.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stopper.asset.entity.Stopper;
import com.stopper.asset.entity.StopperShift;
import com.stopper.asset.mapper.StopperMapper;
import com.stopper.asset.mapper.StopperShiftMapper;
import com.stopper.asset.vo.StopperEquipmentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class StopperService extends ServiceImpl<StopperMapper, Stopper> {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StopperShiftMapper stopperShiftMapper;

    private static final String SPECS_CACHE_KEY = "stopper:specs";
    private static final long SPECS_CACHE_EXPIRE = 24;

    public List<String> getAllSpecs() {
        List<String> specs = (List<String>) redisTemplate.opsForValue().get(SPECS_CACHE_KEY);
        if (specs != null && !specs.isEmpty()) {
            return specs;
        }
        specs = baseMapper.selectAllSpecs();
        if (specs != null && !specs.isEmpty()) {
            redisTemplate.opsForValue().set(SPECS_CACHE_KEY, specs, SPECS_CACHE_EXPIRE, TimeUnit.HOURS);
        }
        return specs;
    }

    public void evictSpecsCache() {
        redisTemplate.delete(SPECS_CACHE_KEY);
    }

    public List<String> getAllStations() {
        return baseMapper.selectAllStations();
    }

    public List<String> getAllEquipments() {
        return baseMapper.selectAllEquipments();
    }

    public Map<String, List<Stopper>> getStoppersGroupByStation() {
        List<Stopper> all = list(new LambdaQueryWrapper<Stopper>()
                .eq(Stopper::getStatus, 1)
                .eq(Stopper::getDeleted, 0)
                .orderByAsc(Stopper::getStation, Stopper::getStopperNo));
        return all.stream().collect(Collectors.groupingBy(Stopper::getStation));
    }

    public Map<String, List<StopperEquipmentVO>> getStoppersGroupByEquipment() {
        List<Stopper> allStoppers = list(new LambdaQueryWrapper<Stopper>()
                .eq(Stopper::getStatus, 1)
                .eq(Stopper::getDeleted, 0)
                .orderByAsc(Stopper::getAdaptEquipment, Stopper::getStopperNo));

        List<Long> stopperIds = allStoppers.stream()
                .map(Stopper::getId)
                .collect(Collectors.toList());

        Map<Long, LocalDateTime> lastShiftTimeMap = new HashMap<>();
        if (!stopperIds.isEmpty()) {
            List<StopperShift> allShifts = stopperShiftMapper.selectList(new LambdaQueryWrapper<StopperShift>()
                    .eq(StopperShift::getDeleted, 0)
                    .in(StopperShift::getStopperId, stopperIds)
                    .orderByDesc(StopperShift::getShiftTime));

            for (StopperShift shift : allShifts) {
                lastShiftTimeMap.putIfAbsent(shift.getStopperId(), shift.getShiftTime());
            }
        }

        List<StopperEquipmentVO> voList = new ArrayList<>();
        for (Stopper stopper : allStoppers) {
            StopperEquipmentVO vo = new StopperEquipmentVO();
            vo.setId(stopper.getId());
            vo.setStopperNo(stopper.getStopperNo());
            vo.setSpec(stopper.getSpec());
            vo.setAdaptEquipment(stopper.getAdaptEquipment());
            vo.setStation(stopper.getStation());
            vo.setLastShiftTime(lastShiftTimeMap.get(stopper.getId()));
            voList.add(vo);
        }

        return voList.stream()
                .collect(Collectors.groupingBy(
                        vo -> vo.getAdaptEquipment() != null ? vo.getAdaptEquipment() : "未适配设备",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    public List<Stopper> getByStation(String station) {
        return baseMapper.selectByStation(station);
    }

    public Stopper getByNo(String stopperNo) {
        return getOne(new LambdaQueryWrapper<Stopper>()
                .eq(Stopper::getStopperNo, stopperNo)
                .eq(Stopper::getDeleted, 0));
    }

    public boolean existsByStopperNo(String stopperNo, Long excludeId) {
        LambdaQueryWrapper<Stopper> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Stopper::getStopperNo, stopperNo)
                .eq(Stopper::getDeleted, 0);
        if (excludeId != null) {
            wrapper.ne(Stopper::getId, excludeId);
        }
        return count(wrapper) > 0;
    }

    public boolean addStopper(Stopper stopper) {
        if (existsByStopperNo(stopper.getStopperNo(), null)) {
            return false;
        }
        stopper.setStatus(1);
        stopper.setStorageTime(LocalDateTime.now());
        stopper.setCreateTime(LocalDateTime.now());
        stopper.setUpdateTime(LocalDateTime.now());
        stopper.setDeleted(0);
        boolean result = save(stopper);
        if (result) {
            evictSpecsCache();
        }
        return result;
    }

    public boolean updateStopper(Stopper stopper) {
        if (existsByStopperNo(stopper.getStopperNo(), stopper.getId())) {
            return false;
        }
        stopper.setUpdateTime(LocalDateTime.now());
        boolean result = updateById(stopper);
        if (result) {
            evictSpecsCache();
        }
        return result;
    }

    public boolean deleteStopper(Long id) {
        Stopper stopper = new Stopper();
        stopper.setId(id);
        stopper.setDeleted(1);
        stopper.setUpdateTime(LocalDateTime.now());
        boolean result = updateById(stopper);
        if (result) {
            evictSpecsCache();
        }
        return result;
    }

    public boolean updateStation(Long id, String station) {
        Stopper stopper = new Stopper();
        stopper.setId(id);
        stopper.setStation(station);
        stopper.setUpdateTime(LocalDateTime.now());
        return updateById(stopper);
    }
}
