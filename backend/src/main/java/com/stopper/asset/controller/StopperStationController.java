package com.stopper.asset.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stopper.asset.common.Result;
import com.stopper.asset.entity.StopperStation;
import com.stopper.asset.service.StopperStationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/station")
public class StopperStationController {

    @Autowired
    private StopperStationService stationService;

    @GetMapping("/names")
    public Result<List<String>> getStationNames(@RequestParam(required = false) String keyword) {
        return Result.success(stationService.searchStations(keyword));
    }

    @GetMapping("/zones")
    public Result<List<String>> getZones() {
        return Result.success(stationService.getAllZones());
    }

    @GetMapping("/page")
    public Result<Page<StopperStation>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                              @RequestParam(required = false) String keyword) {
        return Result.success(stationService.getStationPage(pageNum, pageSize, keyword));
    }

    @GetMapping("/{id}")
    public Result<StopperStation> getById(@PathVariable Long id) {
        return Result.success(stationService.getById(id));
    }

    @GetMapping("/name/{stationName}")
    public Result<StopperStation> getByName(@PathVariable String stationName) {
        return Result.success(stationService.getByStationName(stationName));
    }

    @PostMapping
    public Result<String> add(@RequestBody StopperStation station) {
        if (stationService.existsByStationName(station.getStationName(), null)) {
            return Result.validationError("stationName", "工位名称已存在");
        }
        boolean result = stationService.addStation(station);
        return result ? Result.success("添加工位成功") : Result.error("添加工位失败");
    }

    @PostMapping("/ensure")
    public Result<String> ensureStation(@RequestParam String stationName) {
        boolean result = stationService.ensureStationExists(stationName);
        return result ? Result.success("工位已确保存在") : Result.error("工位确保失败");
    }

    @PutMapping
    public Result<String> update(@RequestBody StopperStation station) {
        if (stationService.existsByStationName(station.getStationName(), station.getId())) {
            return Result.validationError("stationName", "工位名称已存在");
        }
        boolean result = stationService.updateStation(station);
        return result ? Result.success("更新成功") : Result.error("更新失败");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        boolean result = stationService.deleteStation(id);
        return result ? Result.success("删除成功") : Result.error("删除失败");
    }
}
