package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.order.OrderInfo;
import com.sun.org.apache.xpath.internal.operations.Bool;

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

    /**
     * 生成流水号: 用户第一次提交订单生成一个流水号 为防止重复提交
     * @return
     */
    String getTradeNo(String userId);

    /**
     * 比较提交的流水号和缓存流水号 防止重复提交订单
     * @param userId
     * @param tradeNo
     * @return
     */
    Boolean checkTradeNo(String userId,String tradeNo);

    /**
     * 删除缓存流水号
     * @param userId
     */
    void delTradeNo(String userId);

}
