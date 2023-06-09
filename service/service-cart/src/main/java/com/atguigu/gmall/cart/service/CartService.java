package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.model.cart.CartInfo;

import java.util.List;

public interface CartService {

    /**
     *     添加购物车业务
     * @param skuId
     * @param userId
     * @param skuMun
     */
    void addToCart(Long skuId,String userId,Integer skuMun);

    /**
     * 查询用户购物车
     * @param userId
     * @param userTempId
     * @return
     */
    List<CartInfo> getCartList(String userId, String userTempId);

    /**
     * 修改选中状态
     * @param skuId
     * @param isCheckd
     * @param userId
     */
    void checkCart(Long skuId, Integer isChecked, String userId);

    /**
     * 删除购物车
     * @param skuId
     * @param userId
     */
    void deleteCart(Long skuId, String userId);

    /**
     * 获取购物车选中列表
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(Long userId);
}
