package com.atguigu.gmall.order.client.impl;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.client.ServiceOrderFeignClient;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @ClassName: OrderDegradeFeignClient
 * @author: javaermamba
 * @date: 2023-06-2023/6/19-12:14
 * @Description:
 */
@Component
public class OrderDegradeFeignClient implements ServiceOrderFeignClient {


    @Override
    public Result<Map<String, Object>> authTrade() {
        return null;
    }

    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        return null;
    }
}
