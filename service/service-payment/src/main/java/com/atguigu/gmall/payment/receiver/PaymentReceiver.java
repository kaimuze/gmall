package com.atguigu.gmall.payment.receiver;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.payment.service.PaymentService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @ClassName: PaymentReceiver
 * @Project: gmall-parent
 * @Package: com.atguigu.gmall.payment.receiver
 * @Author: carry.kaimuze
 * @Date: 2023-2023/6/21-1:00
 * @Description: mq消息监听者消费者
 */
@Component
public class PaymentReceiver {

    @Autowired
    private PaymentService paymentService;

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAYMENT_PAY,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE),
            key = {MqConst.ROUTING_PAYMENT_CLOSE}
    ))
    public void closePayment(Long orderId, Message message, Channel channel){
        try {
            if (orderId!=null){
                paymentService.closePayment(orderId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

}
