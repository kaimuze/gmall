package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author 恺牧泽
 */

public interface BaseTrademarkService extends IService<BaseTrademark> {
    /**
     * 根据商品id删除商品品牌信息数据
     * @param id
     */
    void removeTrademarkById(Long id);

    /**
     * 保存品牌信息
     * @param baseTrademark
     */
    void saveTrademark(BaseTrademark baseTrademark);
}
