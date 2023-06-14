package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author 恺牧泽
 */
public interface BaseCategoryTrademarkService extends IService<BaseCategoryTrademark> {

    /**
     * 获取品牌分类列表数据
     * @param category3Id
     * @return
     */
    List<BaseTrademark> getTrademarkList(Long category3Id);

    /**
     * 根据当前三级分类id查询分类品牌数据 根据分类id获取可选列表
     * @param category3Id
     * @return
     */
    List<BaseTrademark> getCurrentCategoryTrademarkList(Long category3Id);

    /**
     * 保存品牌分类数据 保存分类与品牌的关系
     * @param categoryTrademarkVo
     */
    void save(CategoryTrademarkVo categoryTrademarkVo);

    /**
     * 删除品牌与分类数据
     * @param category3Id
     * @param trademarkId
     */
    void removeTrademark(Long category3Id, Long trademarkId);
}
