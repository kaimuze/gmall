package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserInfo;

/**
 * @author 恺牧泽
 */
public interface UserService {
    /**
     * 登录功能
     * @param userInfo
     */
    UserInfo login(UserInfo userInfo);
}
