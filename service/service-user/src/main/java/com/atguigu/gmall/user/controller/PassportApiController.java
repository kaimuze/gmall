package com.atguigu.gmall.user.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName: PassportApiController
 * @author: javaermamba
 * @date: 2023-06-2023/6/17-18:39
 * @Description:
 */
@RestController
@RequestMapping("/api/user/passport")
public class PassportApiController {

    @Autowired
    private UserService userService;

    @PostMapping("login")
    public Result login(@RequestBody UserInfo userInfo){
        //登录需要 用户名 密码
        UserInfo info = this.userService.login(userInfo);

        if (info != null){
            return Result.ok();
        }else {
            return Result.fail().message("用户名密码错误");
        }

    }



}
