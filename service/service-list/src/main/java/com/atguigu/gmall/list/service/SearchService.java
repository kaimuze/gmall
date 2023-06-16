package com.atguigu.gmall.list.service;

/**
 * @ClassName: SearchService
 * @author: javaermamba
 * @date: 2023-06-2023/6/16-21:30
 * @Description:
 */

public interface SearchService {

    /**
     * 商品es上架
     * @param skuId
     */
    void upperGoods(Long skuId);

    /**
     * 商品下架
     * @param skuId
     */
    void lowerGoods(Long skuId);


}
