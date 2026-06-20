package com.stopper.asset.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stopper.asset.common.Result;
import com.stopper.asset.entity.Stopper;
import com.stopper.asset.service.StopperService;
import com.stopper.asset.vo.StopperEquipmentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stopper")
public class StopperController {

    @Autowired
    private StopperService stopperService;

    @GetMapping("/list")
    public Result<List<Stopper>> list() {
        return Result.success(stopperService.list(new LambdaQueryWrapper<Stopper>()
                .eq(Stopper::getDeleted, 0)
                .orderByAsc(Stopper::getStopperNo)));
    }

    @GetMapping("/page")
    public Result<Page<Stopper>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                       @RequestParam(defaultValue = "10") Integer pageSize,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) String spec,
                                       @RequestParam(required = false) String station,
                                       @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Stopper> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Stopper::getDeleted, 0);
        if (spec != null && !spec.isEmpty()) {
            wrapper.eq(Stopper::getSpec, spec);
        }
        if (station != null && !station.isEmpty()) {
            wrapper.eq(Stopper::getStation, station);
        }
        if (status != null) {
            wrapper.eq(Stopper::getStatus, status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Stopper::getStopperNo, keyword)
                    .or().like(Stopper::getSpec, keyword)
                    .or().like(Stopper::getAdaptEquipment, keyword));
        }
        wrapper.orderByDesc(Stopper::getCreateTime);
        Page<Stopper> page = new Page<>(pageNum, pageSize);
        return Result.success(stopperService.page(page, wrapper));
    }

    @GetMapping("/group-by-station")
    public Result<Map<String, List<Stopper>>> groupByStation() {
        return Result.success(stopperService.getStoppersGroupByStation());
    }

    @GetMapping("/group-by-equipment")
    public Result<Map<String, List<StopperEquipmentVO>>> groupByEquipment() {
        return Result.success(stopperService.getStoppersGroupByEquipment());
    }

    @GetMapping("/{id}")
    public Result<Stopper> getById(@PathVariable Long id) {
        LambdaQueryWrapper<Stopper> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Stopper::getId, id)
                .eq(Stopper::getDeleted, 0);
        return Result.success(stopperService.getOne(wrapper));
    }

    @GetMapping("/no/{stopperNo}")
    public Result<Stopper> getByNo(@PathVariable String stopperNo) {
        return Result.success(stopperService.getByNo(stopperNo));
    }

    @GetMapping("/specs")
    public Result<List<String>> specs() {
        return Result.success(stopperService.getAllSpecs());
    }

    @GetMapping("/stations")
    public Result<List<String>> stations() {
        return Result.success(stopperService.getAllStations());
    }

    @GetMapping("/equipments")
    public Result<List<String>> equipments() {
        return Result.success(stopperService.getAllEquipments());
    }

    @PostMapping
    public Result<String> add(@RequestBody Stopper stopper) {
        if (stopperService.existsByStopperNo(stopper.getStopperNo(), null)) {
            return Result.validationError("stopperNo", "挡块编号已存在");
        }
        boolean result = stopperService.addStopper(stopper);
        return result ? Result.success("添加成功") : Result.error("添加失败");
    }

    @PutMapping
    public Result<String> update(@RequestBody Stopper stopper) {
        if (stopperService.existsByStopperNo(stopper.getStopperNo(), stopper.getId())) {
            return Result.validationError("stopperNo", "挡块编号已存在");
        }
        boolean result = stopperService.updateStopper(stopper);
        return result ? Result.success("更新成功") : Result.error("更新失败");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        boolean result = stopperService.deleteStopper(id);
        return result ? Result.success("删除成功") : Result.error("删除失败");
    }
}
