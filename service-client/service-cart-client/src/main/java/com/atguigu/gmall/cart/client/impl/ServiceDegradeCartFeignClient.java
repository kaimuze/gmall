package com.atguigu.gmall.cart.client.impl;

import com.atguigu.gmall.cart.client.ServiceCartFeignClient;
import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @ClassName: ServiceDegradeCartFeignClient
 * @author: javaermamba
 * @date: 2023-06-2023/6/19-10:53
 * @Description:
 */
@Component
public class ServiceDegradeCartFeignClient implements ServiceCartFeignClient {

    @Override
    public List<CartInfo> getCartCheckedList(Long userId) {
        return null;
    }
}
