package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;

import java.util.Map;

/**
 * @author carry.kaimuze
 */

public interface PaymentService {

    // 保存交易信息数据  orderInfo -> paymentInfo
    void savePaymentInfo(OrderInfo orderInfo, String paymentType);

    /**
     * 根据outTradeNo paymentType获取交易记录状态
     * @param outTradeNo
     * @param name
     * @return
     */
    PaymentInfo getPaymentInfo(String outTradeNo, String name);

    /**
     * 支付成功更新记录
     * @param outTradeNo
     * @param name
     * @param paramMap
     */
    void paySuccess(String outTradeNo, String name, Map<String, String> paramMap);
}
