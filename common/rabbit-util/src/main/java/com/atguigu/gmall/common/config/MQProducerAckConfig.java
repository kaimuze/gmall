package com.atguigu.gmall.common.config;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;

/**
 * @ClassName: MQProducerAckConfig
 * @author: javaermamba
 * @date: 2023-06-2023/6/20-11:33
 * @Description: 封装发送消息确认
 */

@Component
public class MQProducerAckConfig implements RabbitTemplate.ConfirmCallback,RabbitTemplate.ReturnCallback {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // 初始化方法 类加载后就执行此方法  将消息发送确认类MQProducerAckConfig与RabbitTemplate建立关系,使其生效
    @PostConstruct
    public void init() throws Exception {
        this.rabbitTemplate.setConfirmCallback(this);
        this.rabbitTemplate.setReturnCallback(this);
    }

    /**
     * 消息是否发送到交换器(服务器)
     * @param correlationData
     * @param ack
     * @param code
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String code) {
        if (ack){
            System.out.println("消息发送成功");
        }else {
            System.out.println("消息发送失败"+code);
        }
    }

    /**
     * 消息是否发送到队列,如果消息发送到了队列中,不走这个方法,如果消息没有到达队列,则执行这个方法
     * @param message          消息主体
     * @param replyCode        应答码
     * @param replyText        应答码对应原因
     * @param exchange         交换机
     * @param routingKey       路由键
     */
    @Override
    public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        // 反序列化对象输出
        System.out.println("消息主体: " + new String(message.getBody()));
        System.out.println("应答码: " + replyCode);
        System.out.println("描述：" + replyText);
        System.out.println("消息使用的交换器 exchange : " + exchange);
        System.out.println("消息使用的路由键 routing : " + routingKey);
    }

    /**
     *
     * @param message
     * @param replyCode
     * @param replyText
     * @param exchange
     * @param routingKey
     */
    public void returnedMessage1(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        // 反序列化对象输出
        System.out.println("消息主体: " + new String(message.getBody()));
        System.out.println("应答码: " + replyCode);
        System.out.println("描述：" + replyText);
        System.out.println("消息使用的交换器 exchange : " + exchange);
        System.out.println("消息使用的路由键 routing : " + routingKey);
    }
}
