package com.stopper.asset.controller;

import com.stopper.asset.common.Result;
import com.stopper.asset.entity.Stopper;
import com.stopper.asset.entity.StopperScrap;
import com.stopper.asset.service.StopperScrapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scrap")
public class StopperScrapController {

    @Autowired
    private StopperScrapService scrapService;

    @GetMapping("/list")
    public Result<List<StopperScrap>> list() {
        return Result.success(scrapService.getAllScraps());
    }

    @GetMapping("/{id}")
    public Result<StopperScrap> getById(@PathVariable Long id) {
        return Result.success(scrapService.getScrapById(id));
    }

    @PostMapping
    public Result<Stopper> add(@RequestBody StopperScrap scrap) {
        Stopper stopper = scrapService.addScrap(scrap);
        return stopper != null ? Result.success(stopper) : Result.error("登记失败");
    }

    @GetMapping("/stopper/{stopperId}")
    public Result<List<StopperScrap>> getByStopperId(@PathVariable Long stopperId) {
        return Result.success(scrapService.getScrapsByStopperId(stopperId));
    }
}
