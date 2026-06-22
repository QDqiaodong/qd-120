package com.stopper.asset.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stopper.asset.entity.Stopper;
import com.stopper.asset.entity.StopperShift;
import com.stopper.asset.entity.StopperStation;
import com.stopper.asset.mapper.StopperMapper;
import com.stopper.asset.mapper.StopperShiftMapper;
import com.stopper.asset.mapper.StopperStationMapper;
import com.stopper.asset.vo.StopperEquipmentVO;
import com.stopper.asset.vo.StopperNameplateVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private StopperStationMapper stopperStationMapper;

    @Autowired
    private StopperStationService stopperStationService;

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
        return stopperStationMapper.selectAllStationNames();
    }

    public List<String> getAllEquipments() {
        return baseMapper.selectAllEquipments();
    }

    public Map<String, List<Stopper>> getStoppersGroupByStation() {
        List<Stopper> all = list(new LambdaQueryWrapper<Stopper>()
                .eq(Stopper::getStatus, 1)
                .eq(Stopper::getDeleted, 0)
                .orderByAsc(Stopper::getStation, Stopper::getStopperNo));
        return all.stream().collect(Collectors.groupingBy(
                s -> s.getStation() != null && !s.getStation().isBlank() ? s.getStation() : "未分配工位",
                LinkedHashMap::new,
                Collectors.toList()
        ));
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
                        vo -> vo.getAdaptEquipment() != null && !vo.getAdaptEquipment().isBlank() ? vo.getAdaptEquipment() : "未适配设备",
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

    public Stopper findExistingByStopperNo(String stopperNo, Long excludeId) {
        LambdaQueryWrapper<Stopper> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Stopper::getStopperNo, stopperNo)
                .eq(Stopper::getDeleted, 0);
        if (excludeId != null) {
            wrapper.ne(Stopper::getId, excludeId);
        }
        return getOne(wrapper);
    }

    public String generateStopperNo(String spec) {
        if (spec == null || spec.trim().isEmpty()) {
            throw new RuntimeException("规格不能为空");
        }
        String year = String.valueOf(LocalDateTime.now().getYear());
        String specPrefix = extractSpecPrefix(spec);
        String lastNo = baseMapper.selectLastStopperNoByYearAndPrefix(year, specPrefix);
        int nextSeq = 1;
        if (lastNo != null && !lastNo.isEmpty()) {
            int prefixLength = year.length() + specPrefix.length();
            if (lastNo.length() > prefixLength) {
                try {
                    String seqStr = lastNo.substring(prefixLength);
                    nextSeq = Integer.parseInt(seqStr) + 1;
                } catch (NumberFormatException e) {
                    nextSeq = 1;
                }
            }
        }
        if (nextSeq > 999) {
            throw new RuntimeException("该年份+规格下的编号已超过最大值(999)");
        }
        return year + specPrefix + String.format("%03d", nextSeq);
    }

    private String extractSpecPrefix(String spec) {
        if (spec == null) {
            return "X";
        }
        int idx = spec.indexOf("型");
        if (idx > 0) {
            return spec.substring(0, idx).toUpperCase();
        }
        if (!spec.isEmpty()) {
            return String.valueOf(spec.charAt(0)).toUpperCase();
        }
        return "X";
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean addStopper(Stopper stopper, boolean autoGenerate) {
        if (autoGenerate) {
            String generatedNo = generateStopperNo(stopper.getSpec());
            stopper.setStopperNo(generatedNo);
        }
        if (stopper.getStopperNo() == null || stopper.getStopperNo().trim().isEmpty()) {
            throw new RuntimeException("挡块编号不能为空");
        }
        if (stopper.getSpec() == null || stopper.getSpec().trim().isEmpty()) {
            throw new RuntimeException("规格型号不能为空");
        }
        if (stopper.getStation() == null || stopper.getStation().trim().isEmpty()) {
            throw new RuntimeException("存放工位不能为空");
        }
        stopper.setStopperNo(stopper.getStopperNo().trim());
        if (stopper.getSpec() != null) {
            stopper.setSpec(stopper.getSpec().trim());
        }
        if (stopper.getAdaptEquipment() != null) {
            stopper.setAdaptEquipment(stopper.getAdaptEquipment().trim());
        }
        stopper.setStation(stopper.getStation().trim());
        if (stopper.getRemark() != null) {
            stopper.setRemark(stopper.getRemark().trim());
        }
        if (existsByStopperNo(stopper.getStopperNo(), null)) {
            return false;
        }
        stopper.setStatus(1);
        stopper.setStorageTime(LocalDateTime.now());
        stopper.setCreateTime(LocalDateTime.now());
        stopper.setUpdateTime(LocalDateTime.now());
        stopper.setDeleted(0);
        ensureStationRecorded(stopper.getStation());
        boolean result = save(stopper);
        if (result) {
            evictSpecsCache();
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean addStopper(Stopper stopper) {
        return addStopper(stopper, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateStopper(Stopper stopper) {
        if (stopper.getStopperNo() == null || stopper.getStopperNo().trim().isEmpty()) {
            throw new RuntimeException("挡块编号不能为空");
        }
        if (stopper.getSpec() == null || stopper.getSpec().trim().isEmpty()) {
            throw new RuntimeException("规格型号不能为空");
        }
        if (stopper.getStation() == null || stopper.getStation().trim().isEmpty()) {
            throw new RuntimeException("存放工位不能为空");
        }
        stopper.setStopperNo(stopper.getStopperNo().trim());
        if (stopper.getSpec() != null) {
            stopper.setSpec(stopper.getSpec().trim());
        }
        if (stopper.getAdaptEquipment() != null) {
            stopper.setAdaptEquipment(stopper.getAdaptEquipment().trim());
        }
        stopper.setStation(stopper.getStation().trim());
        if (stopper.getRemark() != null) {
            stopper.setRemark(stopper.getRemark().trim());
        }
        if (existsByStopperNo(stopper.getStopperNo(), stopper.getId())) {
            return false;
        }
        stopper.setUpdateTime(LocalDateTime.now());
        ensureStationRecorded(stopper.getStation());
        boolean result = updateById(stopper);
        if (result) {
            evictSpecsCache();
        }
        return result;
    }

    private void ensureStationRecorded(String stationName) {
        if (stationName == null || stationName.trim().isEmpty()) {
            return;
        }
        String trimmedStation = stationName.trim();
        Long count = stopperStationMapper.selectCount(
                new LambdaQueryWrapper<StopperStation>()
                        .eq(StopperStation::getStationName, trimmedStation)
                        .eq(StopperStation::getDeleted, 0)
        );
        if (count != null && count > 0) {
            return;
        }
        StopperStation newStation = new StopperStation();
        newStation.setStationName(trimmedStation);
        String zone = extractZone(trimmedStation);
        newStation.setZone(zone);
        newStation.setCreateTime(LocalDateTime.now());
        newStation.setUpdateTime(LocalDateTime.now());
        newStation.setDeleted(0);
        stopperStationMapper.insert(newStation);
        stopperStationService.evictStationsCache();
    }

    private String extractZone(String stationName) {
        if (stationName == null) return null;
        int idx = stationName.indexOf("区");
        if (idx > 0) {
            return stationName.substring(0, idx + 1);
        }
        return null;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteStopper(Long id) {
        boolean result = lambdaUpdate()
                .set(Stopper::getDeleted, 1)
                .set(Stopper::getUpdateTime, LocalDateTime.now())
                .eq(Stopper::getId, id)
                .eq(Stopper::getDeleted, 0)
                .update();
        if (result) {
            evictSpecsCache();
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateStation(Long id, String station) {
        ensureStationRecorded(station);
        Stopper stopper = new Stopper();
        stopper.setId(id);
        stopper.setStation(station);
        stopper.setUpdateTime(LocalDateTime.now());
        boolean result = updateById(stopper);
        if (result) {
            stopperStationService.evictStationsCache();
        }
        return result;
    }

    public List<StopperNameplateVO> getNameplatesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<Stopper> stoppers = list(new LambdaQueryWrapper<Stopper>()
                .in(Stopper::getId, ids)
                .eq(Stopper::getDeleted, 0)
                .eq(Stopper::getStatus, 1)
                .orderByAsc(Stopper::getStopperNo));
        List<StopperNameplateVO> voList = new ArrayList<>();
        for (Stopper stopper : stoppers) {
            StopperNameplateVO vo = new StopperNameplateVO();
            vo.setId(stopper.getId());
            vo.setStopperNo(stopper.getStopperNo());
            vo.setSpec(stopper.getSpec());
            vo.setAdaptEquipment(stopper.getAdaptEquipment());
            vo.setStation(stopper.getStation());
            voList.add(vo);
        }
        return voList;
    }
}
