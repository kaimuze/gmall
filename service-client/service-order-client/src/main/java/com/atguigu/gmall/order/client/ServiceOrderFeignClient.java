package com.atguigu.gmall.order.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.order.client.impl.OrderDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author 恺牧泽
 */
@FeignClient(value = "service-order",fallback = OrderDegradeFeignClient.class)
public interface ServiceOrderFeignClient {

    // HttpServletRequest request 远程调用这个对象不需要传递  因为设置了拦截器,已将用户id信息,在进行feign调用时重新放到请求头中了
    @GetMapping("/api/order/auth/trade")
    Result<Map<String,Object>> authTrade();
}
