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
                .eq(Stopper::getStatus, 1)
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

    @GetMapping("/generate-no")
    public Result<String> generateNo(@RequestParam String spec) {
        try {
            String no = stopperService.generateStopperNo(spec);
            return Result.success(no);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping
    public Result<String> add(@RequestBody Map<String, Object> params) {
        Boolean autoGenerate = params.get("autoGenerate") != null ? (Boolean) params.get("autoGenerate") : false;
        Stopper stopper = new Stopper();
        if (params.get("id") != null) {
            stopper.setId(Long.valueOf(params.get("id").toString()));
        }
        stopper.setStopperNo(params.get("stopperNo") != null ? params.get("stopperNo").toString() : null);
        stopper.setSpec(params.get("spec") != null ? params.get("spec").toString() : null);
        stopper.setAdaptEquipment(params.get("adaptEquipment") != null ? params.get("adaptEquipment").toString() : null);
        stopper.setStation(params.get("station") != null ? params.get("station").toString() : null);
        stopper.setImageUrl(params.get("imageUrl") != null ? params.get("imageUrl").toString() : null);
        stopper.setRemark(params.get("remark") != null ? params.get("remark").toString() : null);
        if (!autoGenerate && (stopper.getStopperNo() == null || stopper.getStopperNo().trim().isEmpty())) {
            return Result.validationError("stopperNo", "挡块编号不能为空");
        }
        if (stopper.getSpec() == null || stopper.getSpec().trim().isEmpty()) {
            return Result.validationError("spec", "规格型号不能为空");
        }
        if (stopper.getStation() == null || stopper.getStation().trim().isEmpty()) {
            return Result.validationError("station", "存放工位不能为空");
        }
        if (!autoGenerate && stopperService.existsByStopperNo(stopper.getStopperNo(), null)) {
            return Result.validationError("stopperNo", "挡块编号已存在");
        }
        boolean result;
        try {
            result = stopperService.addStopper(stopper, autoGenerate);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
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
