package com.atguigu.gmall.list.receiver;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.list.service.SearchService;
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
 * @ClassName: ListReceiver
 * @Project: gmall-parent
 * @Package: com.atguigu.gmall.list.receiver
 * @Author: carry.kaimuze
 * @Date: 2023-2023/6/20-13:04
 * @Description: MQ消息监听者(消费者)
 */
@Component
public class ListReceiver {

    @Autowired
    private SearchService searchService;

    /**
     * 商品上架消息消费方法
     * @param skuId
     * @param message
     * @param channel
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_UPPER,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS),
            key = {MqConst.ROUTING_GOODS_UPPER}
    ))
    public void upperGoods(Long skuId, Message message, Channel channel){

        try {
            if (skuId!=null){
                this.searchService.upperGoods(skuId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 消息确认失败 即生产者消息没有被消费者消费或其他异常  第三个参数: 表示是否重回队列
            // 不适用这个的原因: 可能是服务器或者代码异常,即便是重回队列也不能消费消息,容易造成队列堵塞情况
//            channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,true);

            // 如何处理失败的消息呢? 直接做记录,有效信息放入记录表 insert into good_msg(skuId,....);
            // 日志 log: 通过人员查看为何失败,进行检查.

        }

        //消息确认   如果不确认的话,这条消息会一直在队列中
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    /**
     * 商品下架方法
     * @param skuId
     * @param message
     * @param channel
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_LOWER,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS),
            key = {MqConst.ROUTING_GOODS_LOWER}
    ))
    public void lowerGoods(Long skuId, Message message, Channel channel){

        try {
            if (skuId!=null){
                this.searchService.lowerGoods(skuId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 消息确认失败 即生产者消息没有被消费者消费或其他异常  第三个参数: 表示是否重回队列
            // 不适用这个的原因: 可能是服务器或者代码异常,即便是重回队列也不能消费消息,容易造成队列堵塞情况
//            channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,true);

            // 如何处理失败的消息呢? 直接做记录,有效信息放入记录表 insert into good_msg(skuId,....);
            // 日志 log: 通过人员查看为何失败,进行检查.

        }

        //消息确认   如果不确认的话,这条消息会一直在队列中
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

}
