package com.stopper.asset.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stopper.asset.entity.StopperStation;
import com.stopper.asset.mapper.StopperStationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class StopperStationService extends ServiceImpl<StopperStationMapper, StopperStation> {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String STATIONS_CACHE_KEY = "stopper:stations:all";
    private static final long STATIONS_CACHE_EXPIRE = 24;

    public List<String> getAllStationNames() {
        List<String> stations = (List<String>) redisTemplate.opsForValue().get(STATIONS_CACHE_KEY);
        if (stations != null && !stations.isEmpty()) {
            return stations;
        }
        stations = baseMapper.selectAllStationNames();
        if (stations != null && !stations.isEmpty()) {
            redisTemplate.opsForValue().set(STATIONS_CACHE_KEY, stations, STATIONS_CACHE_EXPIRE, TimeUnit.HOURS);
        }
        return stations;
    }

    public List<String> searchStations(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllStationNames();
        }
        return baseMapper.selectStationNamesByKeyword(keyword.trim());
    }

    public List<String> getAllZones() {
        return baseMapper.selectAllZones();
    }

    public Page<StopperStation> getStationPage(Integer pageNum, Integer pageSize, String keyword) {
        LambdaQueryWrapper<StopperStation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StopperStation::getDeleted, 0);
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(StopperStation::getStationName, keyword)
                    .or().like(StopperStation::getZone, keyword));
        }
        wrapper.orderByAsc(StopperStation::getStationName);
        Page<StopperStation> page = new Page<>(pageNum, pageSize);
        return page(page, wrapper);
    }

    public boolean existsByStationName(String stationName, Long excludeId) {
        LambdaQueryWrapper<StopperStation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StopperStation::getStationName, stationName)
                .eq(StopperStation::getDeleted, 0);
        if (excludeId != null) {
            wrapper.ne(StopperStation::getId, excludeId);
        }
        return count(wrapper) > 0;
    }

    public StopperStation getByStationName(String stationName) {
        return getOne(new LambdaQueryWrapper<StopperStation>()
                .eq(StopperStation::getStationName, stationName)
                .eq(StopperStation::getDeleted, 0));
    }

    public boolean addStation(StopperStation station) {
        if (existsByStationName(station.getStationName(), null)) {
            return false;
        }
        station.setCreateTime(LocalDateTime.now());
        station.setUpdateTime(LocalDateTime.now());
        station.setDeleted(0);
        boolean result = save(station);
        if (result) {
            evictStationsCache();
        }
        return result;
    }

    public boolean ensureStationExists(String stationName) {
        if (stationName == null || stationName.trim().isEmpty()) {
            return false;
        }
        StopperStation existing = getByStationName(stationName.trim());
        if (existing != null) {
            return true;
        }
        StopperStation newStation = new StopperStation();
        newStation.setStationName(stationName.trim());
        String zone = extractZone(stationName.trim());
        newStation.setZone(zone);
        return addStation(newStation);
    }

    private String extractZone(String stationName) {
        if (stationName == null) return null;
        int idx = stationName.indexOf("区");
        if (idx > 0) {
            return stationName.substring(0, idx + 1);
        }
        return null;
    }

    public boolean updateStation(StopperStation station) {
        if (existsByStationName(station.getStationName(), station.getId())) {
            return false;
        }
        station.setUpdateTime(LocalDateTime.now());
        boolean result = updateById(station);
        if (result) {
            evictStationsCache();
        }
        return result;
    }

    public boolean deleteStation(Long id) {
        StopperStation station = new StopperStation();
        station.setId(id);
        station.setDeleted(1);
        station.setUpdateTime(LocalDateTime.now());
        boolean result = updateById(station);
        if (result) {
            evictStationsCache();
        }
        return result;
    }

    public void evictStationsCache() {
        redisTemplate.delete(STATIONS_CACHE_KEY);
    }
}
