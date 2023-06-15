package com.atguigu.gmall.item.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.impl.ItemDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author 恺牧泽
 */
@FeignClient(value = "service-item",fallback = ItemDegradeFeignClient.class)
public interface ItemFeignClient {

    /**
     * 为web-all模块提供远程调用地址and数据
     * @param skuId
     * @return
     */
    @GetMapping("/api/item/{skuId}")
    public Result skuItem(@PathVariable Long skuId);

}
