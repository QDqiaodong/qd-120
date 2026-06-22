package com.stopper.asset.controller;

import com.stopper.asset.common.Result;
import com.stopper.asset.entity.Stopper;
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
    public Result<Stopper> add(@RequestBody StopperMaintenance maintenance) {
        Stopper stopper = maintenanceService.sendToMaintenance(maintenance);
        return stopper != null ? Result.success(stopper) : Result.error("登记失败");
    }

    @PostMapping("/{id}/complete")
    public Result<Stopper> complete(@PathVariable Long id, @RequestBody StopperMaintenance completeRequest) {
        Stopper stopper = maintenanceService.completeMaintenance(id, completeRequest);
        return stopper != null ? Result.success(stopper) : Result.error("处理失败");
    }
}
