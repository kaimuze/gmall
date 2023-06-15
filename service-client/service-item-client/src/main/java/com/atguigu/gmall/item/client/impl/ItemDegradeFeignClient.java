package com.atguigu.gmall.item.client.impl;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.ItemFeignClient;
import org.springframework.stereotype.Component;

/**
 * @ClassName: ItemDegreadeFeignClient
 * @author: javaermamba
 * @date: 2023-06-2023/6/15-20:33
 * @Description:
 */
@Component
public class ItemDegradeFeignClient implements ItemFeignClient {

    @Override
    public Result skuItem(Long skuId) {
        return null;
    }
}
