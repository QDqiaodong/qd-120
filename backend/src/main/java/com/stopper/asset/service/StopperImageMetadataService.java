package com.stopper.asset.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stopper.asset.entity.StopperImageMetadata;
import com.stopper.asset.mapper.StopperImageMetadataMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class StopperImageMetadataService extends ServiceImpl<StopperImageMetadataMapper, StopperImageMetadata> {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String IMAGE_METADATA_CACHE_PREFIX = "stopper:image:metadata:";
    private static final long CACHE_EXPIRE_HOURS = 24;

    public List<StopperImageMetadata> getByStopperNo(String stopperNo) {
        String cacheKey = IMAGE_METADATA_CACHE_PREFIX + stopperNo;
        List<StopperImageMetadata> result = (List<StopperImageMetadata>) redisTemplate.opsForValue().get(cacheKey);
        if (result != null) {
            return result;
        }
        result = list(new LambdaQueryWrapper<StopperImageMetadata>()
                .eq(StopperImageMetadata::getStopperNo, stopperNo)
                .eq(StopperImageMetadata::getDeleted, 0)
                .orderByDesc(StopperImageMetadata::getUploadTime));
        redisTemplate.opsForValue().set(cacheKey, result, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean addImageMetadata(StopperImageMetadata metadata) {
        if (metadata.getImageUrl() == null || metadata.getImageUrl().trim().isEmpty()) {
            throw new RuntimeException("图片地址不能为空");
        }
        metadata.setImageUrl(metadata.getImageUrl().trim());
        if (metadata.getStopperNo() != null) {
            metadata.setStopperNo(metadata.getStopperNo().trim());
        }
        metadata.setUploadTime(LocalDateTime.now());
        metadata.setCreateTime(LocalDateTime.now());
        metadata.setUpdateTime(LocalDateTime.now());
        metadata.setDeleted(0);
        boolean result = save(metadata);
        if (result && metadata.getStopperNo() != null) {
            evictImageMetadataCache(metadata.getStopperNo());
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateImageMetadata(StopperImageMetadata metadata) {
        if (metadata.getId() == null) {
            throw new RuntimeException("ID不能为空");
        }
        metadata.setUpdateTime(LocalDateTime.now());
        String oldStopperNo = getOldStopperNo(metadata.getId());
        boolean result = updateById(metadata);
        if (result) {
            if (oldStopperNo != null) {
                evictImageMetadataCache(oldStopperNo);
            }
            if (metadata.getStopperNo() != null && !metadata.getStopperNo().equals(oldStopperNo)) {
                evictImageMetadataCache(metadata.getStopperNo());
            }
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteImageMetadata(Long id) {
        String stopperNo = getOldStopperNo(id);
        boolean result = lambdaUpdate()
                .set(StopperImageMetadata::getDeleted, 1)
                .set(StopperImageMetadata::getUpdateTime, LocalDateTime.now())
                .eq(StopperImageMetadata::getId, id)
                .eq(StopperImageMetadata::getDeleted, 0)
                .update();
        if (result && stopperNo != null) {
            evictImageMetadataCache(stopperNo);
        }
        return result;
    }

    private String getOldStopperNo(Long id) {
        StopperImageMetadata old = getById(id);
        return old != null ? old.getStopperNo() : null;
    }

    public void evictImageMetadataCache(String stopperNo) {
        redisTemplate.delete(IMAGE_METADATA_CACHE_PREFIX + stopperNo);
    }
}
