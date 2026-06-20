package com.stopper.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stopper.asset.entity.StopperStation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StopperStationMapper extends BaseMapper<StopperStation> {

    @Select("SELECT DISTINCT station_name FROM stopper_station WHERE deleted = 0 ORDER BY station_name")
    List<String> selectAllStationNames();

    @Select("SELECT DISTINCT station_name FROM stopper_station WHERE deleted = 0 AND station_name LIKE CONCAT('%', #{keyword}, '%') ORDER BY station_name")
    List<String> selectStationNamesByKeyword(@Param("keyword") String keyword);

    @Select("SELECT DISTINCT zone FROM stopper_station WHERE deleted = 0 AND zone IS NOT NULL ORDER BY zone")
    List<String> selectAllZones();
}
