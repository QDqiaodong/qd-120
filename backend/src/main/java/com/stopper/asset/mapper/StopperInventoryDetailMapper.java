package com.stopper.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stopper.asset.entity.StopperInventoryDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StopperInventoryDetailMapper extends BaseMapper<StopperInventoryDetail> {

    @Select("SELECT d.* FROM stopper_inventory_detail d " +
            "INNER JOIN stopper s ON d.stopper_id = s.id " +
            "WHERE d.inventory_id = #{inventoryId} AND d.deleted = 0 AND s.deleted = 0")
    List<StopperInventoryDetail> selectByInventoryId(@Param("inventoryId") Long inventoryId);
}
