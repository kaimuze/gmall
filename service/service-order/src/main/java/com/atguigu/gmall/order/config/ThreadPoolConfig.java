package com.atguigu.gmall.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName: ThreadPoolConfig
 * @author: javaermamba
 * @date: 2023-06-2023/6/16-17:38
 * @Description: 多线程并发多任务组合用线程池
 */
@Configuration
public class ThreadPoolConfig {

    @Bean
    public ThreadPoolExecutor threadPoolExecutor(){
        return new ThreadPoolExecutor(
                5,
                100,
                3,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(30)
        );
    }

}
