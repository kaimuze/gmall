package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.order.OrderInfo;

/**
 * @author carry.kaimuze
 */

public interface PaymentService {

    // 保存交易信息数据  orderInfo -> paymentInfo
    void savePaymentInfo(OrderInfo orderInfo, String paymentType);

}
