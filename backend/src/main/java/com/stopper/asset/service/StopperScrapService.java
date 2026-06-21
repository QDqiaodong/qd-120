package com.stopper.asset.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stopper.asset.entity.Stopper;
import com.stopper.asset.entity.StopperScrap;
import com.stopper.asset.mapper.StopperScrapMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StopperScrapService extends ServiceImpl<StopperScrapMapper, StopperScrap> {

    @Autowired
    private StopperService stopperService;

    @Transactional(rollbackFor = Exception.class)
    public boolean addScrap(StopperScrap scrap) {
        Stopper stopper = stopperService.getOne(new LambdaQueryWrapper<Stopper>()
                .eq(Stopper::getId, scrap.getStopperId())
                .eq(Stopper::getDeleted, 0));
        if (stopper == null) {
            throw new RuntimeException("挡块不存在或已删除");
        }

        if (stopper.getStatus() != null && stopper.getStatus() == 2) {
            throw new RuntimeException("该挡块已报废，不可重复报废");
        }

        long existingScrapCount = count(new LambdaQueryWrapper<StopperScrap>()
                .eq(StopperScrap::getStopperId, scrap.getStopperId())
                .eq(StopperScrap::getDeleted, 0));
        if (existingScrapCount > 0) {
            throw new RuntimeException("该挡块已存在报废记录");
        }

        scrap.setStopperNo(stopper.getStopperNo());
        scrap.setSpec(stopper.getSpec());
        scrap.setScrapTime(LocalDateTime.now());
        scrap.setCreateTime(LocalDateTime.now());
        scrap.setDeleted(0);

        boolean scrapResult = save(scrap);
        if (!scrapResult) {
            return false;
        }

        stopper.setStatus(2);
        stopper.setUpdateTime(LocalDateTime.now());
        return stopperService.updateById(stopper);
    }

    public List<StopperScrap> getAllScraps() {
        return list(new LambdaQueryWrapper<StopperScrap>()
                .eq(StopperScrap::getDeleted, 0)
                .orderByDesc(StopperScrap::getScrapTime));
    }

    public StopperScrap getScrapById(Long id) {
        return getById(id);
    }

    public List<StopperScrap> getScrapsByStopperId(Long stopperId) {
        return list(new LambdaQueryWrapper<StopperScrap>()
                .eq(StopperScrap::getStopperId, stopperId)
                .eq(StopperScrap::getDeleted, 0)
                .orderByDesc(StopperScrap::getScrapTime));
    }
}
