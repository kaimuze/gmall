package com.atguigu.gmall.item.service;

import com.atguigu.gmall.model.item.ItemVo;

/**
 * @author 恺牧泽
 */

public interface ItemService {

    /**
     * 根据skuId获取Item商品数据信息
     * @param skuId
     * @return
     */
    ItemVo getItem(Long skuId);
}
