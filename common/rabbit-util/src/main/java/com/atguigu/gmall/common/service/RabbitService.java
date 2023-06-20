package com.atguigu.gmall.common.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @ClassName: RabbitService
 * @author: javaermamba
 * @date: 2023-06-2023/6/20-11:28
 * @Description: 封装消息发送
 */
@Service
public class RabbitService {

    // 引入发送消息的模板
    @Autowired
    private RabbitTemplate rabbitTemplate;


    /**
     * // 封装发送消息的方法
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param msg        发送的消息
     * @return
     */
    public Boolean sendMes(String exchange, String routingKey, Object msg) {

        // 发送消息
        rabbitTemplate.convertAndSend(exchange, routingKey, msg);
        //默认返回true
        return true;
    }

    /**
     * // 封装发送延迟消息的方法  优化取消订单业务  在保存订单的时候发送延迟消息v
     * @param exchange
     * @param routingKey
     * @param msg
     * @param delayTime
     * @return
     */
    public Boolean sendDelayMsg(String exchange, String routingKey, Object msg, int delayTime) {

        // 发送消息
        // 基于死信的时候,在队列中设置的TTL时间. 插件不需要
        this.rabbitTemplate.convertAndSend(exchange, routingKey, msg, message -> {
            // 设置延迟时间 10s
            message.getMessageProperties().setDelay(delayTime * 1000);
            return message;
        });

        //默认返回数据
        return true;
    }

}
