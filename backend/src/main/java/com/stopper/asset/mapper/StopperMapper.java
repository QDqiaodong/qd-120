package com.stopper.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stopper.asset.entity.Stopper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StopperMapper extends BaseMapper<Stopper> {

    @Select("SELECT DISTINCT spec FROM stopper WHERE deleted = 0 ORDER BY spec")
    List<String> selectAllSpecs();

    @Select("SELECT DISTINCT station FROM stopper WHERE deleted = 0 ORDER BY station")
    List<String> selectAllStations();

    @Select("SELECT DISTINCT adapt_equipment FROM stopper WHERE deleted = 0 ORDER BY adapt_equipment")
    List<String> selectAllEquipments();

    @Select("SELECT * FROM stopper WHERE deleted = 0 AND station = #{station} ORDER BY stopper_no")
    List<Stopper> selectByStation(@Param("station") String station);

    @Select("SELECT stopper_no FROM stopper WHERE deleted = 0 AND stopper_no LIKE CONCAT(#{year}, #{specPrefix}, '%') ORDER BY stopper_no DESC LIMIT 1")
    String selectLastStopperNoByYearAndPrefix(@Param("year") String year, @Param("specPrefix") String specPrefix);
}
