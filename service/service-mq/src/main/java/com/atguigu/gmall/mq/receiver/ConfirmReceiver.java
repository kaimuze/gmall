package com.atguigu.gmall.mq.receiver;

import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @ClassName: ConfirmReceiver
 * @Project: gmall-parent
 * @Package: com.atguigu.gmall.mq.receiver
 * @Author: 恺牧泽
 * @Date: 2023-2023/6/20-12:27
 * @Description: mq消息消费者
 */
@Component
public class ConfirmReceiver {

    //监听消息
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue.confirm", durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = "exchange.confirm"),
            key = {"routing.confirm"}
    ))
    public void getMsg(String msg, Message message, Channel channel) {

        //处理消息
        System.out.println(msg);
        System.out.println("消息主体接收:" + new String(message.getBody()));

        //消息的确认
        // (消息中的标识,是否批量确认/一次确认一条消息还是批量确认)
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }


}
