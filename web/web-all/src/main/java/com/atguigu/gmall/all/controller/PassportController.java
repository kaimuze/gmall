package com.atguigu.gmall.all.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * @ClassName: PassportController
 * @author: javaermamba
 * @date: 2023-06-2023/6/17-20:10
 * @Description: 登录页面控制器
 */
@Controller
public class PassportController {

    @GetMapping("login.html")
    public String login(HttpServletRequest request){
        //originUrl: [[${originUrl}]],// 后台存储这么一个originUrl
        // originUrl: 表示用户是从哪里点击的登录地址,如果登录成功之后,还要跳回哪里
        String originUrl = request.getParameter("originUrl");
        request.setAttribute("originUrl",originUrl);
        return "login";
    }

}
