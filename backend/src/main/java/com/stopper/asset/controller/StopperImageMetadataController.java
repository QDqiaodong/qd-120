package com.stopper.asset.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stopper.asset.common.Result;
import com.stopper.asset.entity.StopperImageMetadata;
import com.stopper.asset.service.StopperImageMetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/image-metadata")
public class StopperImageMetadataController {

    @Autowired
    private StopperImageMetadataService imageMetadataService;

    @GetMapping("/page")
    public Result<Page<StopperImageMetadata>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "10") Integer pageSize,
                                                   @RequestParam(required = false) String stopperNo,
                                                   @RequestParam(required = false) String source) {
        LambdaQueryWrapper<StopperImageMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StopperImageMetadata::getDeleted, 0);
        if (stopperNo != null && !stopperNo.isEmpty()) {
            wrapper.eq(StopperImageMetadata::getStopperNo, stopperNo);
        }
        if (source != null && !source.isEmpty()) {
            wrapper.eq(StopperImageMetadata::getSource, source);
        }
        wrapper.orderByDesc(StopperImageMetadata::getUploadTime);
        Page<StopperImageMetadata> page = new Page<>(pageNum, pageSize);
        return Result.success(imageMetadataService.page(page, wrapper));
    }

    @GetMapping("/stopper/{stopperNo}")
    public Result<List<StopperImageMetadata>> getByStopperNo(@PathVariable String stopperNo) {
        return Result.success(imageMetadataService.getByStopperNo(stopperNo));
    }

    @GetMapping("/{id}")
    public Result<StopperImageMetadata> getById(@PathVariable Long id) {
        LambdaQueryWrapper<StopperImageMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StopperImageMetadata::getId, id)
                .eq(StopperImageMetadata::getDeleted, 0);
        return Result.success(imageMetadataService.getOne(wrapper));
    }

    @PostMapping
    public Result<String> add(@RequestBody StopperImageMetadata metadata) {
        try {
            boolean result = imageMetadataService.addImageMetadata(metadata);
            return result ? Result.success("添加成功") : Result.error("添加失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping
    public Result<String> update(@RequestBody StopperImageMetadata metadata) {
        try {
            boolean result = imageMetadataService.updateImageMetadata(metadata);
            return result ? Result.success("更新成功") : Result.error("更新失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        try {
            boolean result = imageMetadataService.deleteImageMetadata(id);
            return result ? Result.success("删除成功") : Result.error("删除失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
