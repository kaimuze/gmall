package com.atguigu.gmall.order.receiver;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.service.OrderService;
import com.google.common.collect.PeekingIterator;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @ClassName: OrderReceiver
 * @Project: gmall-parent
 * @Package: com.atguigu.gmall.order.receiver
 * @Author: carry.kaimuze
 * @Date: 2023-2023/6/20-14:37
 * @Description: 延迟插件, 监听延迟消息
 */
@Component
public class OrderReceiver {

    @Autowired
    private OrderService orderService;

    @SneakyThrows
    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    public void orderCancel(Long orderId, Message message, Channel channel) {
        try {
            if (orderId != null) {
                //查询订单是否支付
                OrderInfo orderInfo = orderService.getById(orderId);
                // 判断订单状态和支付状态
                if (orderInfo != null && "UNPAID".equals(orderInfo.getOrderStatus()) && "UNPAID".equals(orderInfo.getProcessStatus())) {
                    // 取消订单业务
                    orderService.execExpireOrder(orderId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();

        }

        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    // 订单监听支付成功之后消息,更新订单状态
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAYMENT_PAY, declare = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_PAY),
            key = {MqConst.ROUTING_PAYMENT_PAY}
    ))
    public void updateOrderStatus(Long orderId, Message message, Channel channel) {

        try {
            if (orderId != null) {
                //确保orderInfo不为空
                OrderInfo orderInfo = this.orderService.getById(orderId);
                if (orderInfo != null && "UNPAID".equals(orderInfo.getOrderStatus())) {
                    //更具订单id更新订单状态
                    orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
                }
            }
        } catch (Exception e) {
            // 异常记录到日志表
            e.printStackTrace();
        }

        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

}
