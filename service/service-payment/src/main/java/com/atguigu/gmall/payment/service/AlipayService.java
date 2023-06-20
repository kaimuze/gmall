package com.atguigu.gmall.payment.service;

import com.alipay.api.AlipayApiException;

/**
 * @author carry.kaimuze
 */
public interface AlipayService {

    //生成支付二维码
    String createAlipay(Long orderId) throws AlipayApiException;

    /**
     * 退款接口
     * @param orderId
     * @return
     */
    Boolean refund(Long orderId);
}
