package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.order.client.ServiceOrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * @ClassName: OrderController
 * @author: javaermamba
 * @date: 2023-06-2023/6/19-11:10
 * @Description: 结算页面控制器
 */
@Controller
public class OrderController {

    @Qualifier("com.atguigu.gmall.order.client.ServiceOrderFeignClient")
    @Autowired
    private ServiceOrderFeignClient orderFeignClient;

    // http://order.gmall.com/trade.html
    @GetMapping("trade.html")
    public String trade(Model model){

        Result<Map<String, Object>> result = orderFeignClient.authTrade();
        Map<String, Object> data = result.getData();
        model.addAllAttributes(data);

        return "order/trade";
    }



}
