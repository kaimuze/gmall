package com.atguigu.gmall.user.service.impl;

import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.mapper.UserInfoMapper;
import com.atguigu.gmall.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * @ClassName: UserServiceImpl
 * @author: javaermamba
 * @date: 2023-06-2023/6/17-18:52
 * @Description:
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Override
    public UserInfo login(UserInfo userInfo) {
        //select * from user_info where login_name = ? and passwd = ?;
        LambdaQueryWrapper<UserInfo> userInfoLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userInfoLambdaQueryWrapper.eq(UserInfo::getLoginName,userInfo.getLoginName());
        // 密码加密处理  111111 -> sdkhfwhehsadkjghwfasd23r
        String newPwd = DigestUtils.md5DigestAsHex(userInfo.getPasswd().getBytes(StandardCharsets.UTF_8));
        userInfoLambdaQueryWrapper.eq(UserInfo::getPasswd,newPwd);

        UserInfo info = this.userInfoMapper.selectOne(userInfoLambdaQueryWrapper);
        if (info != null){
            return info;
        }

        return null;

    }

}
