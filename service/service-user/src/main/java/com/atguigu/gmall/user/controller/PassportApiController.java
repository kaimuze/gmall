package com.atguigu.gmall.user.controller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.IpUtil;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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

    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping("login")
    public Result login(@RequestBody UserInfo userInfo, HttpServletRequest request) {
        //登录需要 用户名 密码
        UserInfo info = this.userService.login(userInfo);

        if (info != null) {

            //生成 token 存到 cookie中
            String token = UUID.randomUUID().toString();
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("token", token);
            // 登录完成之后,需要在页面显示用户昵称 写入cookie
            hashMap.put("nickName", info.getNickName());


            //用户id存入缓存 判断用户是否登录的关键点
            // key=user:login:token
            String userLoginKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userId", info.getId().toString());
            jsonObject.put("ip", IpUtil.getIpAddress(request));

            this.redisTemplate.opsForValue().set(userLoginKey, jsonObject.toJSONString(), RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);


            return Result.ok(hashMap);
        } else {
            return Result.fail().message("用户名密码错误");
        }

    }

    @GetMapping("loginout")
    public Result loginout(HttpServletRequest request){
        //本质删除 缓存数据 删除cookie中的数据
        // 前端代码将token存在了cookie 和 header中
        String token = request.getHeader("token");
        String userLoginKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;
        this.redisTemplate.delete(userLoginKey);

        //删除cookie 在js中实现了
        return Result.ok();
    }


}
