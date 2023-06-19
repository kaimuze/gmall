package com.atguigu.gmall.user.client.impl;

import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.user.client.UserFeignClient;

import java.util.List;

/**
 * @ClassName: UserDegradeFeignClientImpl
 * @author: javaermamba
 * @date: 2023-06-2023/6/19-9:47
 * @Description:
 */

public class UserDegradeFeignClientImpl implements UserFeignClient {

    @Override
    public List<UserAddress> findUserAddressListByUserId(Long userId) {
        return null;
    }
}
