package com.atguigu.gmall.item.cotroller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.model.item.ItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName: ItemApiController
 * @author: javaermamba
 * @date: 2023-06-2023/6/15-14:39
 * @Description: feign 远程调用接口
 */
@RestController
@RequestMapping("/api/item")
public class ItemApiController {

    @Autowired
    private ItemService itemService;

    /**
     * 为web-all模块提供远程调用地址and数据
     * @param skuId
     * @return
     */
    @GetMapping("{skuId}")
    public Result skuItem(@PathVariable Long skuId) {
        ItemVo itemVo = this.itemService.getItem(skuId);
        return Result.ok(itemVo);
    }

}
