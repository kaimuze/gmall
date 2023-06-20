package com.atguigu.gmall.mq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * @ClassName: ServiceMqApplication
 * @author: javaermamba
 * @date: 2023-06-2023/6/20-10:53
 * @Description: RabbitMQ测试模块
 */

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)//取消数据源自动配置
@ComponentScan({"com.atguigu.gmall"})
@EnableDiscoveryClient
public class ServiceMqApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceMqApplication.class, args);
    }
}

