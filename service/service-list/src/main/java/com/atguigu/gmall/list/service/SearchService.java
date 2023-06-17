package com.atguigu.gmall.list.service;

import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;

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


    //商品热度访问流程  web-all -> Gateway -> service-item -> service-list
    /**
     * 记录商品热度排名
     * @param skuId
     */
    void incrHotScore(Long skuId);

    /**
     * es检索数据方法
     * @param searchParam
     * @return
     */
    SearchResponseVo search(SearchParam searchParam) throws Exception;
}
