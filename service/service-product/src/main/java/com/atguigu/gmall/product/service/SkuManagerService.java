package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.SkuInfo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * @author 恺牧泽
 */

public interface SkuManagerService {

    /**
     * 保存sku数据
     * @param skuInfo
     */
    void saveSkuInfo(SkuInfo skuInfo);

    /**
     * 根据三级分类id分页查询sku信息数据
     * @param pageModel
     * @param category3Id
     * @return
     */
    IPage<SkuInfo> getListSku(Page<SkuInfo> pageModel, Long category3Id);

    /**
     * 商品上架
     * @param skuId
     */
    void onSale(Long skuId);

    /**
     * 商品下架
     * @param skuId
     */
    void cancelSale(Long skuId);
}
