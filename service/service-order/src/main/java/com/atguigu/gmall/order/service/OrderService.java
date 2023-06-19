package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.order.OrderInfo;

/**
 * @author 恺牧泽
 */
public interface OrderService {

    /**
     * 保存订单信息
     * @param orderInfo
     * @return
     */
    Long saveOrderInfo(OrderInfo orderInfo);
}
