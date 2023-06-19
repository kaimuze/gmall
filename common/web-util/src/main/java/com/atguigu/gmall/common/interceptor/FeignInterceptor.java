package com.atguigu.gmall.common.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 请求拦截器: 在feign远程调用的时候,请求中不包含请求头信息,但是项目中我们将用户id信息放到了请求头中为后面的微服务调用提供数据支持
 *              使用请求拦截器将feign远程调用的请求头中,再次放入用户信息解决
 */
@Component
public class FeignInterceptor implements RequestInterceptor {

    public void apply(RequestTemplate requestTemplate){
            //  微服务远程调用使用feign ，feign 传递数据的时候，没有。
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            //  添加header 数据
            requestTemplate.header("userTempId", request.getHeader("userTempId"));
            requestTemplate.header("userId", request.getHeader("userId"));

    }

}
