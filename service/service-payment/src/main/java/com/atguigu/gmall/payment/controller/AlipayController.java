package com.atguigu.gmall.payment.controller;

import com.alipay.api.AlipayApiException;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName: AlipayController
 * @Project: gmall-parent
 * @Package: com.atguigu.gmall.payment.controller
 * @Author: carry.kaimuze
 * @Date: 2023-2023/6/20-19:19
 * @Description:
 */
@Controller
@RequestMapping("/api/payment/alipay")
public class AlipayController {

    @Autowired
    private AlipayService alipayService;

    // http://api.gmall.com/api/payment/alipay/submit/49
    @GetMapping("/submit/{orderId}")
    @ResponseBody
    public String aliPay(@PathVariable Long orderId){

        String form = null;
        try {
            form = this.alipayService.createAlipay(orderId);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        //将form输出到页面
        return form;
    }


    //同步回调地址: http://api.gmall.com/api/payment/alipay/callback/return
    @GetMapping("callback/return")
    public String callbackReturn(){
        //重定向到web-all控制器
        // http://payment.gmall.com/pay/success.html
        return "redirect:" + AlipayConfig.return_order_url;

    }


}
