package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserAddress;

import java.util.List;

/**
 * @author 恺牧泽
 */
public interface UserAddressService {


    /**
     * 购物车结算:获取用户地址信息
     * @param userId
     * @return
     */
    List<UserAddress> findUserAddressListByUserId(Long userId);
}
