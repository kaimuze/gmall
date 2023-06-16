package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.ItemFeignClient;
import com.atguigu.gmall.model.item.ItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @ClassName: ItemController
 * @author: javaermamba
 * @date: 2023-06-2023/6/15-21:02
 * @Description:
 */
@Controller
public class ItemController {

    @Qualifier("com.atguigu.gmall.item.client.ItemFeignClient")
    @Autowired
    private ItemFeignClient itemFeignClient;

    @GetMapping("{skuId}.html")
    public String skuItem(@PathVariable Long skuId, Model model) {

        Result<Map> result = itemFeignClient.skuItem(skuId);

        model.addAllAttributes(result.getData());
        // 返回商品详情视图名称
        return "item/item";
    }



}
