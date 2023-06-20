package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sun.org.apache.xpath.internal.operations.Bool;

/**
 * @author 恺牧泽
 */
public interface OrderService extends IService<OrderInfo> {

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

    /**
     * 验证库存
     * @param skuId
     * @param skuNum
     * @return
     */
    Boolean checkStock(Long skuId, Integer skuNum);

    /**
     * 获取我的订单列表数据
     * @param pageModel
     * @param userId
     * @return
     */
    IPage<OrderInfo> getOrderInfoPage(Page<OrderInfo> pageModel, String userId);

    /**
     * 取消订单操作
     * @param orderId
     */
    void execExpireOrder(Long orderId);

    /**
     * 根据订单id获取订单集合数据
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(Long orderId);

    /**
     * 更新订单方法          // 后续会有很多根据订单id更新订单状态和进程状态的需求. 因此做一个方法抽离.
     * @param orderId
     * @param processStatus
     */
    void updateOrderStatus(Long orderId, ProcessStatus processStatus);
}
