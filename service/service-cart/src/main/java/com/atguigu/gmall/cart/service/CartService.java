package com.atguigu.gmall.cart.service;

public interface CartService {

    // 添加购物车业务
    void addToCart(Long skuId,String userId,Integer skuMun);
}
