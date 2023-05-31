package com.atguigu.gmall.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * @ClassName: ServerGatewayApplication
 * @author: javaermamba
 * @date: 2023-05-2023/5/31-16:46
 * @Description:
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ServerGatewayApplication {
    public static void main(String[] args) {

        SpringApplication.run(ServerGatewayApplication.class,args);
    }

}
