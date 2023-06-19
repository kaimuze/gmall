package com.atguigu.gmall.user.service.impl;

import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.user.mapper.UserAddressServiceMapper;
import com.atguigu.gmall.user.service.UserAddressService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @ClassName: UserAddressServiceImpl
 * @author: javaermamba
 * @date: 2023-06-2023/6/19-9:40
 * @Description:
 */
@Service
public class UserAddressServiceImpl implements UserAddressService {

    @Autowired
    private UserAddressServiceMapper userAddressServiceMapper;

    @Override
    public List<UserAddress> findUserAddressListByUserId(Long userId) {
        LambdaQueryWrapper<UserAddress> userAddressLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userAddressLambdaQueryWrapper.eq(UserAddress::getUserId,userId);
        return userAddressServiceMapper.selectList(userAddressLambdaQueryWrapper);
    }
}
