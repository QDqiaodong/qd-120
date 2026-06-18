package com.stopper.asset;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@MapperScan("com.stopper.asset.mapper")
@EnableCaching
public class StopperAssetApplication {

    public static void main(String[] args) {
        SpringApplication.run(StopperAssetApplication.class, args);
    }
}
