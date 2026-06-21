package com.stopper.asset.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stopper.asset.entity.Stopper;
import com.stopper.asset.entity.StopperMaintenance;
import com.stopper.asset.entity.StopperScrap;
import com.stopper.asset.exception.ValidationException;
import com.stopper.asset.mapper.StopperMaintenanceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StopperMaintenanceService extends ServiceImpl<StopperMaintenanceMapper, StopperMaintenance> {

    @Autowired
    private StopperService stopperService;

    @Autowired
    private StopperScrapService stopperScrapService;

    private static final String MAINTENANCE_STATION = "维修区";
    private static final Integer STATUS_IN_REPAIR = 1;
    private static final Integer STATUS_COMPLETED = 2;
    private static final Integer STATUS_SCRAPPED = 2;
    private static final String OUTCOME_RETURN = "RETURN";
    private static final String OUTCOME_SCRAP = "SCRAP";

    @Transactional(rollbackFor = Exception.class)
    public boolean sendToMaintenance(StopperMaintenance maintenance) {
        Stopper stopper = stopperService.getOne(new LambdaQueryWrapper<Stopper>()
                .eq(Stopper::getId, maintenance.getStopperId())
                .eq(Stopper::getDeleted, 0));
        if (stopper == null) {
            throw new RuntimeException("挡块不存在或已删除");
        }

        Map<String, String> fieldErrors = new HashMap<>();

        if (STATUS_SCRAPPED.equals(stopper.getStatus())) {
            fieldErrors.put("stopperId", "报废挡块不可送修");
        }

        if (MAINTENANCE_STATION.equals(stopper.getStation())) {
            fieldErrors.put("stopperId", "该挡块已在维修区，无需重复送修");
        }

        if (maintenance.getRepairReason() == null || maintenance.getRepairReason().trim().isEmpty()) {
            fieldErrors.put("repairReason", "请填写维修原因");
        }

        if (maintenance.getExpectedReturnDate() == null) {
            fieldErrors.put("expectedReturnDate", "请选择预计返回日期");
        }

        long activeCount = count(new LambdaQueryWrapper<StopperMaintenance>()
                .eq(StopperMaintenance::getStopperId, maintenance.getStopperId())
                .eq(StopperMaintenance::getStatus, STATUS_IN_REPAIR)
                .eq(StopperMaintenance::getDeleted, 0));
        if (activeCount > 0) {
            fieldErrors.put("stopperId", "该挡块已在维修中，请先完成当前维修");
        }

        if (!fieldErrors.isEmpty()) {
            throw new ValidationException("送修校验失败", fieldErrors);
        }

        maintenance.setStopperNo(stopper.getStopperNo());
        maintenance.setSpec(stopper.getSpec());
        maintenance.setAdaptEquipment(stopper.getAdaptEquipment());
        maintenance.setOriginalStation(stopper.getStation());
        maintenance.setSendTime(LocalDateTime.now());
        maintenance.setStatus(STATUS_IN_REPAIR);
        maintenance.setCreateTime(LocalDateTime.now());
        maintenance.setDeleted(0);

        boolean saveResult = save(maintenance);
        if (!saveResult) {
            return false;
        }

        return stopperService.updateStation(maintenance.getStopperId(), MAINTENANCE_STATION);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean completeMaintenance(Long id, StopperMaintenance completeRequest) {
        StopperMaintenance maintenance = getOne(new LambdaQueryWrapper<StopperMaintenance>()
                .eq(StopperMaintenance::getId, id)
                .eq(StopperMaintenance::getDeleted, 0));
        if (maintenance == null) {
            throw new RuntimeException("维修记录不存在");
        }
        if (!STATUS_IN_REPAIR.equals(maintenance.getStatus())) {
            throw new RuntimeException("该维修记录已完成，不可重复处理");
        }

        String outcome = completeRequest.getOutcome();
        if (outcome == null || (!OUTCOME_RETURN.equals(outcome) && !OUTCOME_SCRAP.equals(outcome))) {
            Map<String, String> fieldErrors = new HashMap<>();
            fieldErrors.put("outcome", "请选择处理结果");
            throw new ValidationException("完成维修校验失败", fieldErrors);
        }

        if (OUTCOME_RETURN.equals(outcome)) {
            String returnStation = completeRequest.getReturnStation();
            if (returnStation == null || returnStation.trim().isEmpty()) {
                returnStation = maintenance.getOriginalStation();
            }
            if (returnStation == null || returnStation.trim().isEmpty()) {
                Map<String, String> fieldErrors = new HashMap<>();
                fieldErrors.put("returnStation", "缺少返回工位，请指定返回工位");
                throw new ValidationException("完成维修校验失败", fieldErrors);
            }
            maintenance.setReturnStation(returnStation.trim());
            stopperService.updateStation(maintenance.getStopperId(), returnStation.trim());
        } else {
            StopperScrap scrap = new StopperScrap();
            scrap.setStopperId(maintenance.getStopperId());
            String reason = maintenance.getRepairReason();
            scrap.setScrapReason((reason != null && !reason.trim().isEmpty())
                    ? "维修后报废：" + reason
                    : "维修后报废");
            scrap.setScrapDegree("严重");
            scrap.setOperator(completeRequest.getCompleteOperator());
            scrap.setRemark("由维修周转转入报废");
            stopperScrapService.addScrap(scrap);
            maintenance.setReturnStation(null);
        }

        maintenance.setStatus(STATUS_COMPLETED);
        maintenance.setOutcome(outcome);
        maintenance.setCompleteTime(LocalDateTime.now());
        maintenance.setCompleteOperator(completeRequest.getCompleteOperator());
        maintenance.setCompleteRemark(completeRequest.getCompleteRemark());
        return updateById(maintenance);
    }

    public List<StopperMaintenance> getAllMaintenance() {
        List<StopperMaintenance> list = list(new LambdaQueryWrapper<StopperMaintenance>()
                .eq(StopperMaintenance::getDeleted, 0)
                .orderByDesc(StopperMaintenance::getSendTime));
        return filterByStopperExists(list);
    }

    public List<StopperMaintenance> getActiveMaintenance() {
        List<StopperMaintenance> list = list(new LambdaQueryWrapper<StopperMaintenance>()
                .eq(StopperMaintenance::getDeleted, 0)
                .eq(StopperMaintenance::getStatus, STATUS_IN_REPAIR)
                .orderByDesc(StopperMaintenance::getSendTime));
        return filterByStopperExists(list);
    }

    public StopperMaintenance getMaintenanceById(Long id) {
        StopperMaintenance maintenance = getById(id);
        if (maintenance == null) {
            return null;
        }
        Stopper stopper = stopperService.getOne(new LambdaQueryWrapper<Stopper>()
                .eq(Stopper::getId, maintenance.getStopperId())
                .eq(Stopper::getDeleted, 0));
        return stopper != null ? maintenance : null;
    }

    public List<StopperMaintenance> getMaintenanceByStopperId(Long stopperId) {
        return list(new LambdaQueryWrapper<StopperMaintenance>()
                .eq(StopperMaintenance::getStopperId, stopperId)
                .eq(StopperMaintenance::getDeleted, 0)
                .orderByDesc(StopperMaintenance::getSendTime));
    }

    private List<StopperMaintenance> filterByStopperExists(List<StopperMaintenance> list) {
        if (list == null || list.isEmpty()) {
            return list;
        }
        List<Long> stopperIds = list.stream()
                .map(StopperMaintenance::getStopperId)
                .distinct()
                .collect(Collectors.toList());
        List<Stopper> existingStoppers = stopperService.list(new LambdaQueryWrapper<Stopper>()
                .in(Stopper::getId, stopperIds)
                .eq(Stopper::getDeleted, 0));
        Set<Long> existingIds = existingStoppers.stream()
                .map(Stopper::getId)
                .collect(Collectors.toSet());
        return list.stream()
                .filter(m -> existingIds.contains(m.getStopperId()))
                .collect(Collectors.toList());
    }
}
