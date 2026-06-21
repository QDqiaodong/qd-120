package com.stopper.asset.controller;

import com.stopper.asset.common.Result;
import com.stopper.asset.entity.StopperMaintenance;
import com.stopper.asset.service.StopperMaintenanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance")
public class StopperMaintenanceController {

    @Autowired
    private StopperMaintenanceService maintenanceService;

    @GetMapping("/list")
    public Result<List<StopperMaintenance>> list() {
        return Result.success(maintenanceService.getAllMaintenance());
    }

    @GetMapping("/active")
    public Result<List<StopperMaintenance>> active() {
        return Result.success(maintenanceService.getActiveMaintenance());
    }

    @GetMapping("/{id}")
    public Result<StopperMaintenance> getById(@PathVariable Long id) {
        return Result.success(maintenanceService.getMaintenanceById(id));
    }

    @GetMapping("/stopper/{stopperId}")
    public Result<List<StopperMaintenance>> getByStopper(@PathVariable Long stopperId) {
        return Result.success(maintenanceService.getMaintenanceByStopperId(stopperId));
    }

    @PostMapping
    public Result<String> add(@RequestBody StopperMaintenance maintenance) {
        boolean result = maintenanceService.sendToMaintenance(maintenance);
        return result ? Result.success("送修登记成功") : Result.error("登记失败");
    }

    @PostMapping("/{id}/complete")
    public Result<String> complete(@PathVariable Long id, @RequestBody StopperMaintenance completeRequest) {
        boolean result = maintenanceService.completeMaintenance(id, completeRequest);
        return result ? Result.success("维修完成处理成功") : Result.error("处理失败");
    }
}
