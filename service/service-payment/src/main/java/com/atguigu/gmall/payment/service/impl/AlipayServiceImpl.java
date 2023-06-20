package com.atguigu.gmall.payment.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.client.ServiceOrderFeignClient;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * @ClassName: AlipayServiceImpl
 * @Project: gmall-parent
 * @Package: com.atguigu.gmall.payment.service.impl
 * @Author: carry.kaimuze
 * @Date: 2023-2023/6/20-19:06
 * @Description:
 */
@Service
public class AlipayServiceImpl implements AlipayService {

    @Autowired
    private AlipayClient alipayClient;


    @Qualifier("com.atguigu.gmall.order.client.ServiceOrderFeignClient")
    @Autowired
    private ServiceOrderFeignClient serviceOrderFeignClient;

    @Autowired
    private PaymentService paymentService;


    @Override
    public String createAlipay(Long orderId) throws AlipayApiException {
        //根据订单id参数,获取orderInfo数据
        OrderInfo orderInfo = serviceOrderFeignClient.getOrderInfo(orderId);

        // 如果取消了订单,就不能继续支付了
        // 需要判断当前订单状态
        if ("CLOSED".equals(orderInfo.getProcessStatus()) || "PAID".equals(orderInfo.getOrderStatus())) {
            return "当前订单已经支付或已关闭";
        }

        //保存交易记录
        paymentService.savePaymentInfo(orderInfo, PaymentType.ALIPAY.name());


        // 已经制作了相关的alipay配置类
        //AlipayClient alipayClient =  new DefaultAlipayClient( "https://openapi.alipay.com/gateway.do" , APP_ID, APP_PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE);  //获得初始化的AlipayClient
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest(); //创建API对应的request
        //同步回调地址: http://api.gmall.com/api/payment/alipay/callback/return
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        //异步回调地址: http://rjsh38.natappfree.cc/api/payment/alipay/callback/notify
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url); //在公共参数中设置回跳和通知地址


        // 构建json,封装后端支付必填数据
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("out_trade_no", orderInfo.getOutTradeNo());
        jsonObject.put("total_amount", "0.01"); // 模拟支付0.01
        jsonObject.put("subject", orderInfo.getTradeBody());
        jsonObject.put("product_code", "FAST_INSTANT_TRADE_PAY");
        // 可先设置二维码有效期
        // 需要做一下判断
        // 订单的过期时间 > 二维码的有效时间 那就设置二维码的有效时间
        // 订单的过期时间 < 二维码的有效时间   设置订单的过期时间
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.MINUTE, 10);
        jsonObject.put("time_expire", simpleDateFormat.format(instance.getTime()));// 绝对超时时间
        //jsonObject.put("timeout_express",)//相对超时时间


        // 封装数据
        alipayRequest.setBizContent(jsonObject.toJSONString());
//        alipayRequest.setBizContent( "{"  +
//                "    \"out_trade_no\":\"20150320010101001\","  +
//                "    \"product_code\":\"FAST_INSTANT_TRADE_PAY\","  +
//                "    \"total_amount\":88.88,"  +
//                "    \"subject\":\"Iphone6 16G\","  +
//                "    \"body\":\"Iphone6 16G\","  +
//                "    \"passback_params\":\"merchantBizType%3d3C%26merchantBizNo%3d2016010101111\","  +
//                "    \"extend_params\":{"  +
//                "    \"sys_service_provider_id\":\"2088511833207846\""  +
//                "    }" +
//                "  }" ); //填充业务参数

        return alipayClient.pageExecute(alipayRequest).getBody();  //调用SDK生成表单
    }

    @Override
    public Boolean refund(Long orderId) {
        //  获取订单对象。
        OrderInfo orderInfo = this.serviceOrderFeignClient.getOrderInfo(orderId);
        // AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        bizContent.put("refund_amount", 0.01);
        bizContent.put("out_request_no", "HZ01RF001");
        //// 返回参数选项，按需传入
        //JSONArray queryOptions = new JSONArray();
        //queryOptions.add("refund_detail_item_list");
        //bizContent.put("query_options", queryOptions);

        request.setBizContent(bizContent.toString());
        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if (response.isSuccess()) {
            System.out.println("调用成功");
            //  在此更新交易记录状态.
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setPaymentStatus(PaymentStatus.CLOSED.name());
            this.paymentService.updatePaymentInfo(orderInfo.getOutTradeNo(), PaymentType.ALIPAY.name(), paymentInfo);
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }

    }
}

