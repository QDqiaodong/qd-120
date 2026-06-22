package com.stopper.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stopper.asset.entity.StopperInventoryReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StopperInventoryReviewMapper extends BaseMapper<StopperInventoryReview> {

    @Select("SELECT r.* FROM stopper_inventory_review r " +
            "WHERE r.inventory_id = #{inventoryId} AND r.deleted = 0 " +
            "ORDER BY r.review_time DESC")
    List<StopperInventoryReview> selectByInventoryId(@Param("inventoryId") Long inventoryId);

    @Select("SELECT r.* FROM stopper_inventory_review r " +
            "WHERE r.detail_id = #{detailId} AND r.deleted = 0 " +
            "ORDER BY r.review_time DESC")
    List<StopperInventoryReview> selectByDetailId(@Param("detailId") Long detailId);

    @Select("SELECT r.* FROM stopper_inventory_review r " +
            "WHERE r.stopper_id = #{stopperId} AND r.deleted = 0 " +
            "ORDER BY r.review_time DESC")
    List<StopperInventoryReview> selectByStopperId(@Param("stopperId") Long stopperId);
}
