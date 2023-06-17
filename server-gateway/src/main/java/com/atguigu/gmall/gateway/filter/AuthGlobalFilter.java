package com.atguigu.gmall.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.IpUtil;
import com.google.errorprone.annotations.Var;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

/**
 * @ClassName: AuthGlobalFilter
 * @author: javaermamba
 * @date: 2023-06-2023/6/17-20:30
 * @Description: 网关过滤器
 *  用户发送的所有请求都先经过网关,在网关中鉴权
 */
@Component
public class AuthGlobalFilter implements GlobalFilter {

    // 声明一个路径格式匹配的对象
    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    //获取nacos配置文件中的控制器
    @Value("${authUrls.url}")
    private String authUrls; //trade.html,myOrder.html,list.html      #addCart.html # 用户访问该控制器的时候，会被拦截跳转到登录！

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        /*
        1. 防止用户通过浏览器直接访问到服务内部的数据接口 api/product/inner/getSkuInfo/24 这样的路径要拦截(带inner的)
            a. 先获取到用户发送的请求URL路径
            b. 判断URI中是否符合内部数据接口的特性
            c. 如果符合内部数据接口特征,则返回信息提示
        2. 限制用户访问带有/auth 这样的控制器资源,访问这样的资源,必须要先登录
            a. 先获取到用户发送的请求URL路径
            b. 判断URI中是否符合内部数据接口的特性
            c. 如果符合内部数据接口特征,则返回信息提示
        3. 限制用户访问哪些控制器, 例如: 访问 trade.html myOrder.html这样的控制器时.必须要登录,如果没有登录,则跳转到登录页面

         */

        //获取用户请求
        ServerHttpRequest request = exchange.getRequest();
        // http://localhost:8206/api/product/inner/getSkuInfo/24
        // request.getURI()   http://localhost:8206/api/product/inner/getSkuInfo/24
        String path = request.getURI().getPath(); // /api/product/inner/getSkuInfo/24

        //路径格式匹配  match("定义的匹配规则",获取到的URI)
        if (antPathMatcher.match("/**/inner/**",path)){
            //匹配到内部数据接口的规则,阻止访问,返回信息提示
            //获取响应对象
            ServerHttpResponse response = exchange.getResponse();
            // 返回信息提示
            return out(response, ResultCodeEnum.PERMISSION);
        }

        // 获取到用户id,以此来判断该用户是否登录了
        String userId = this.getUserId(request);

        // 获取临时用户id
        String userTempId = this.getUserTempId(request);

        // 如果用户id 为-1
        if ("-1".equals(userId)){
            //获取响应对象
            ServerHttpResponse response = exchange.getResponse();
            // 返回信息提示
            return out(response, ResultCodeEnum.PERMISSION);
        }

        // 判断,路径是否符合规则
        if (antPathMatcher.match("/api/**/auth/**",path)){
            //再判断用户是否登录了
            if (StringUtils.isEmpty(userId)){
                //获取响应对象
                ServerHttpResponse response = exchange.getResponse();
                //用户id为空 信息提示需要登录
                return out(response,ResultCodeEnum.LOGIN_AUTH);
            }
        }

        // 3. 限制用户访问哪些控制器, 例如: 访问 trade.html myOrder.html这样的控制器时.必须要登录,如果没有登录,则跳转到登录页面
        // 对配置文件中的url控制器进行分割   传过来的:trade.html,myOrder.html,list.html是一个整体的字符串
        // 好比 用户通过三级分类id访问数据的的url路径是,用户访问的控制器是: http://list.gmall.com/list.html?category3Id=61 这时就要判断出该路径是包含我们限制的控制器list.html是在内的
        // 这时我们就要判断用户id是存在的,不为空的,要是登录状态,如果不是登录状态,就要跳转到登录页面
        // 将字符串进行分割
        String[] split = authUrls.split(",");
        // 判断
        if (split!=null && split.length>0){
            //循环每一个分割出来的控制器,进行匹配
            for (String url : split) {
                //判断Path中,是否包含url中的控制器   path : list.html?category3Id=61 这一部分
                // indexOf找到会返回对应的下标位置,如果没有找到返回-1
                // 请求路径中包含 list.html 或 trade.html 或 myOrder.html 但是用户id是空的,属于未登录状态,需要进行页面提示跳转到登录页面
                if (path.indexOf(url)!=-1 && StringUtils.isEmpty(userId)){
                    // 做页面提示跳转 登录页面
                    //获取响应对象
                    ServerHttpResponse response = exchange.getResponse();
                    // 设置重定项参数
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    // 设置重定项到哪里去 set(本地,)
                    // http://passport.gmall.com/login.html?originUrl=http://list.gmall.com/list.html?keyword=%E6%89%8B%E6%9C%BA
                    response.getHeaders().set(HttpHeaders.LOCATION,"http://passport.gmall.com/login.html?originUrl="+request.getURI());
                    // 重定向
                    return response.setComplete();
                }
            }
        }

        //获取到用户id返回,判断          获取数据,并将数据放入到请求头
        // 再加入获取临时用户id返回的判断  获取数据,并将数据放入到请求头
        if (!StringUtils.isEmpty(userId) || StringUtils.isEmpty(userTempId)){
            if (!StringUtils.isEmpty(userId)){
                // 如果用户id不为空,需要将用户id存储到请求头中,然后在后台各个微服务中获取就可以了
                // 返回值要和方法的返回值Moon<Void>联系到一起才行
                // ServerHttpRequest : 只有build才能返回这个对象
                request.mutate().header("userId", userId).build();
            }
            if (!StringUtils.isEmpty(userTempId)){
                request.mutate().header("userTempId", userTempId).build();
            }

            // 将request ---> exchange
            return chain.filter(exchange.mutate().request(request).build());
        }

        // 默认返回
        return chain.filter(exchange);
    }


    /**
     * 获取临时用户id
     * @param request
     * @return
     */
    private String getUserTempId(ServerHttpRequest request) {
        // 获取临时用户id 并返回
        //点击加入购物车时, 将临时用户id 存储到cookie中
        String userTempId = "";
        HttpCookie httpCookie = request.getCookies().getFirst("userTempId");
        if (httpCookie!=null){
            userTempId = httpCookie.getValue();
        }else {
            //还有一个地方,登录按钮处,前端定义了,如果cookie中有临时用户id,也要将临时用户id放入到header中
            List<String> stringList = request.getHeaders().get("userTempId");
            if (!CollectionUtils.isEmpty(stringList)){
                userTempId = stringList.get(0);
            }
        }

        //返回临时用户id
        return userTempId;
    }


    // 获取用户id数据方法
    private String getUserId(ServerHttpRequest request) {
        // 用户id在缓存中. 要想在缓存中获取数据,就必须要组成缓存的key!
        // key = user:login:token
        String token = "";
        // token 存在cookie中 或 header中   Cookie cookie = new Cookie("token",token);
        // getFirst("token") : 代表去的key中的第一个数据
        //从header中获取key
        List<String> stringList = request.getHeaders().get("token");
        if (!CollectionUtils.isEmpty(stringList)){
            token  = stringList.get(0);
        }else {
            //从cookie中获取key
            HttpCookie httpCookie = request.getCookies().getFirst("token");
            if (httpCookie!=null){
                token = httpCookie.getValue();
            }
        }

        // 判断token是否为空 用户没有登录过 token就为null
        if (!StringUtils.isEmpty(token)){
            //组成缓存的key
            String key = "user:login:" + token;
            // 从缓存中获取数据
            //   "{\"ip\":\"192.168.200.1\",\"userId\":\"2\"}"
            String strJson = (String) this.redisTemplate.opsForValue().get(key);
            //这个字符串原本是 JSONObject,需要转换过来
            JSONObject jsonObject = JSONObject.parseObject(strJson);
            // 获取缓存中的IP
            String ip = (String) jsonObject.get("ip");
            // 获取当前操作主机的IP地址
            String currentIp = IpUtil.getGatwayIpAddress(request);
            // 判断IP一致性
            if (ip.equals(currentIp)){
                String userId = (String) jsonObject.get("userId");
                //返回用户id
                return userId;
            }else {

                return "-1";
            }
        }
        //默认返回空字符串
        return "";
    }



    /**
     *    信息提示方法
     * @param response
     * @param resultCodeEnum
     * @return
     */
    private Mono<Void> out(ServerHttpResponse response, ResultCodeEnum resultCodeEnum) {
        //给用户友好的提示,提示我们已经封装到ResultCodeEnum类中了
        Result<Object> result = Result.build(null,resultCodeEnum);
        // 如何把对象输出到页面? 序列化可以,还可以
        // 将result变成字符串
        String str = JSON.toJSONString(result);
        // 将字符串转换为DataBuffer数据流, 获取到了数据流, 输出页面信息时有中文.
        DataBuffer wrap = response.bufferFactory().wrap(str.getBytes());
        // 有中文怎么办? 设置请求头
        response.getHeaders().add("Content-Type","application/json;charset=UTF-8");
        // 页面要如何输出数据 使用response
        // writeWith(DataBuffer数据流)
        return response.writeWith(Mono.just(wrap));
    }
}
