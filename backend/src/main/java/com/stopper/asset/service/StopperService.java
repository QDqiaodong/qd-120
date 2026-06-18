package com.stopper.asset.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stopper.asset.entity.Stopper;
import com.stopper.asset.mapper.StopperMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class StopperService extends ServiceImpl<StopperMapper, Stopper> {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

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

    public List<Stopper> getByStation(String station) {
        return baseMapper.selectByStation(station);
    }

    public Stopper getByNo(String stopperNo) {
        return getOne(new LambdaQueryWrapper<Stopper>()
                .eq(Stopper::getStopperNo, stopperNo)
                .eq(Stopper::getDeleted, 0));
    }

    public boolean addStopper(Stopper stopper) {
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
