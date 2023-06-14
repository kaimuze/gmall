package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * @author 恺牧泽
 */

public interface ManagerService {

    /**
     * 查询所有一级分类数据
     * @return
     */
    List<BaseCategory1> getCategory1();

    /**
     * 根据一级分类id获取2级分类列表数据
     * @param category1Id
     * @return
     */
    List<BaseCategory2> getCategory2(Long category1Id);

    /**
     * 根据2及分类id查询三级分类数据
     * @param category2Id
     * @return
     */
    List<BaseCategory3> getCategory3(Long category2Id);

    /**
     * 根据分类等级查询对应平台属性数据
     * @param category1Id
     * @param category2Id
     * @param category3Id
     * @return
     */
    List<BaseAttrInfo> getAttrInfoListByCategoryId(Long category1Id, Long category2Id, Long category3Id);

    /**
     * 保存平台属性数据
     * @param baseAttrInfo
     */
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    /**
     * 根据平台属性id查询平台属性值数据(回显)
     * @param attrId
     * @return
     */
    List<BaseAttrValue> getAttrValueList(Long attrId);

    /**
     * 根据平台属性id获取平台属性对象
     * @param attrId
     * @return
     */
    BaseAttrInfo getBaseAttrInfo(Long attrId);

    /**
     * 分页查询SpuInfo数据
     * @param pageModel
     * @param spuInfo
     * @return
     */
    IPage<SpuInfo> getSpuInfoList(Page<SpuInfo> pageModel, SpuInfo spuInfo);

    /**
     * 获取品牌列表数据
     * @param pageModel
     * @return
     */
    IPage<BaseTrademark> getTrademarkList(Page<BaseTrademark> pageModel);
}
