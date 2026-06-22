package com.stopper.asset.controller;

import com.stopper.asset.common.Result;
import com.stopper.asset.entity.Stopper;
import com.stopper.asset.entity.StopperShift;
import com.stopper.asset.service.StopperShiftService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shift")
public class StopperShiftController {

    @Autowired
    private StopperShiftService shiftService;

    @GetMapping("/list")
    public Result<List<StopperShift>> list() {
        return Result.success(shiftService.getAllShifts());
    }

    @GetMapping("/stopper/{stopperId}")
    public Result<List<StopperShift>> getByStopper(@PathVariable Long stopperId) {
        return Result.success(shiftService.getShiftList(stopperId));
    }

    @PostMapping
    public Result<Stopper> add(@RequestBody StopperShift shift) {
        Stopper stopper = shiftService.addShift(shift);
        return stopper != null ? Result.success(stopper) : Result.error("登记失败");
    }
}
