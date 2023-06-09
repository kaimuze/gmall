package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sun.org.apache.xpath.internal.operations.Bool;

import java.util.List;
import java.util.Map;

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
     * 取消订单操作 后续加入了支付和支付宝模块. 存在只需要关闭本地订单业务,而支付和支付宝无需关闭情况,这里创建方法进行区分
     * @param orderId
     */
    void execExpireOrder(Long orderId,String flag);

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

    /**
     * 根据订单号,查询订单信息,并封装通知减库存信息所需数据
     * @param orderId
     */
    void sendOrderStatus(Long orderId);

    /**
     * OrderInfo -> map
     * @param orderInfo
     * @return
     */
    Map initWareOrder(OrderInfo orderInfo);

    /**
     * 拆单业务
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    List<OrderInfo> orderSplit(String orderId, String wareSkuMap);
}
