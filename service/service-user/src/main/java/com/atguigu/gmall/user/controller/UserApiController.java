package com.atguigu.gmall.user.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.user.service.UserAddressService;
import com.atguigu.gmall.user.service.UserService;
import org.redisson.RedissonSubList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @ClassName: UserApiController
 * @author: javaermamba
 * @date: 2023-06-2023/6/19-9:35
 * @Description: 用户送货地址控制器
 */
@RestController
@RequestMapping("/api/user/inner/")
public class UserApiController {

    @Autowired
    private UserAddressService userAddressService;

    @GetMapping("inner/findUserAddressListByUserId/{userId}")
    public List<UserAddress> findUserAddressListByUserId(@PathVariable Long userId) {

        List<UserAddress> userAddressList = this.userAddressService.findUserAddressListByUserId(userId);

        return userAddressList;

    }

}
