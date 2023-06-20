package com.atguigu.gmall.payment.client.impl;

import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.client.PaymentFeignClient;
import org.springframework.stereotype.Component;

/**
 * @ClassName: PaymentDegradeFeignClient
 * @Project: gmall-parent
 * @Package: com.atguigu.gmall.payment.client.impl
 * @Author: carry.kaimuze
 * @Date: 2023-2023/6/21-1:38
 * @Description:
 */
@Component
public class PaymentDegradeFeignClient implements PaymentFeignClient {

    @Override
    public Boolean closeAliPay(Long orderId) {
        return null;
    }

    @Override
    public Boolean checkPayment(Long orderId) {
        return null;
    }

    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo) {
        return null;
    }
}
