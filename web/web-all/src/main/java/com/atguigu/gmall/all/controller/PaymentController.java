package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.client.ServiceOrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * @ClassName: PaymentController
 * @Project: gmall-parent
 * @Package: com.atguigu.gmall.all.controller
 * @Author: carry.kaimuze
 * @Date: 2023-2023/6/20-18:21
 * @Description:
 */
@Controller
public class PaymentController {

    @Qualifier("com.atguigu.gmall.order.client.ServiceOrderFeignClient")
    @Autowired
    private ServiceOrderFeignClient serviceOrderFeignClient;

    // http://payment.gmall.com/pay.html?orderId=35;
    @GetMapping("pay.html")
    public String pay(HttpServletRequest request){

        String orderId = request.getParameter("orderId");
        OrderInfo orderInfo = serviceOrderFeignClient.getOrderInfo(Long.parseLong(orderId));
        request.setAttribute("orderInfo",orderInfo);
        return "/payment/pay";
    }
}
