package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.SkuInfo;

/**
 * @author 恺牧泽
 */

public interface SkuManagerService {

    /**
     * 保存sku数据
     * @param skuInfo
     */
    void saveSkuInfo(SkuInfo skuInfo);
}
