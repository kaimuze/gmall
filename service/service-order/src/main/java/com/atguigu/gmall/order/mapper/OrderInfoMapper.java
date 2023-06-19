package com.atguigu.gmall.order.mapper;

import com.atguigu.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author 恺牧泽
 */
@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    /**
     * 查询我的订单列表
     * @param pageModel
     * @param userId
     * @return
     */
    IPage<OrderInfo> selectOrderInfoPageList(@Param("pageModel") Page<OrderInfo> pageModel, @Param("userId") String userId);
}
