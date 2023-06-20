package com.atguigu.gmall.mq.cotroller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.service.RabbitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ClassName: MqController
 * @BelongsProject: gmall-parent
 * @BelongsPackage: com.atguigu.gmall.mq.cotroller
 * @Author: 恺牧泽
 * @Date: 2023-2023/6/20-12:22
 * @Description: mq消息发送者端
 */
@RestController
@RequestMapping("mq")
public class MqController {

    @Autowired
    private RabbitService rabbitService;

    // 发送消息的控制
    @GetMapping("sendConfirm")
    public Result sendConfirm(){
        rabbitService.sendMes("exchange.confirm","routing.confirm99","ojbk");
        return Result.ok();
    }


}
