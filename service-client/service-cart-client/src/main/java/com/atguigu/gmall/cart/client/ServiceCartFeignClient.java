package com.atguigu.gmall.cart.client;

import com.atguigu.gmall.cart.client.impl.ServiceDegradeCartFeignClient;
import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * @author 恺牧泽
 */
@FeignClient(value = "service-cart", fallback = ServiceDegradeCartFeignClient.class)
public interface ServiceCartFeignClient {

    @GetMapping("/api/cart/getCartCheckedList/{userId}")
    List<CartInfo> getCartCheckedList(@PathVariable Long userId);
}
