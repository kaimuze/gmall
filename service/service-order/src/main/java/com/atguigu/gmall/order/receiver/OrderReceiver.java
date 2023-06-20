package com.atguigu.gmall.order.receiver;

import com.alibaba.fastjson.JSON;
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
import org.springframework.util.StringUtils;

import java.util.Map;

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

    /**
     * 监听取消订单消息 死信队列插件版
     * @param orderId
     * @param message
     * @param channel
     */
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

                    // 订单状态修改完成后,发送消息异步消息到库存系统,通知减库存操作
                    // 库存系统接收参数为JSON格式
                    // 发送消息给库存,通知减库存  通过上面监听的消息获取到orderId,再查到orderInfo数据进行封装后,异步发送消息到库存系统
                    this.orderService.sendOrderStatus(orderId);

                }
            }
        } catch (Exception e) {
            // 异常记录到日志表
            e.printStackTrace();
        }

        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    // 监听减库存的处理结果消息
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_WARE_ORDER,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_WARE_ORDER),
            key = {MqConst.ROUTING_WARE_ORDER}
    ))
    public void updOrder(String jsonStr,Message message,Channel channel){
        try {
            if (!StringUtils.isEmpty(jsonStr)){
                Map map = JSON.parseObject(jsonStr, Map.class);
                String orderId = (String) map.get("orderId");
                String status = (String) map.get("status");
                // 再次判断
                if ("DEDUCTED".equals(status)){
                    //扣减库存成功 更新订单状态
                    this.orderService.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.WAITING_DELEVER);
                }else {
                    this.orderService.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.STOCK_EXCEPTION);
                    // 处理方式两种: 1.补货 2.人工客服介入
                }
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

}
