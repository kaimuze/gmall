package com.atguigu.gmall.order.config;

import com.atguigu.gmall.common.constant.MqConst;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

/**
 * @ClassName: DelayOrderConfig
 * @Project: gmall-parent
 * @Package: com.atguigu.gmall.order.config
 * @Author: carry.kaimuze
 * @Date: 2023-2023/6/20-14:35
 * @Description: 延迟插件:发送延迟消息
 */
@Configuration
public class DelayOrderConfig {

    // 创建交换机
    @Bean
    public CustomExchange delayExchange(){

        //设置额外参数  设置插件的队列类型
        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put("x-delayed-type","direct");
        return new CustomExchange(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL,"x-delayed-message",true,false,hashMap);

    }

    @Bean
    public Queue delayQueue(){

        return new Queue(MqConst.QUEUE_ORDER_CANCEL,true,false,false);
    }

    @Bean
    public Binding delayBinding(){
        // 队列绑定交换机,通过路由键
        return BindingBuilder.bind(delayQueue()).to(delayExchange()).with(MqConst.ROUTING_ORDER_CANCEL).noargs();
    }

}
