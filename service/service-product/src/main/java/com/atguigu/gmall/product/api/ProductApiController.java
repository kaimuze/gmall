package com.atguigu.gmall.product.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: ProductApiController
 * @author: javaermamba
 * @date: 2023-06-2023/6/15-15:00
 * @Description: 远程调用接口, item->product 对外接口
 */
// /api/product/inner/getSkuInfo/{skuId}
@RestController
@RequestMapping("/api/product/")
public class ProductApiController {

    @Autowired
    private ManagerService managerService;

    @GetMapping("/inner/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable Long skuId) {
        return managerService.getSkuInfo(skuId);
    }

    //根据三级分类id获取分类数据
    @GetMapping("/inner/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id) {
        return this.managerService.getCategoryView(category3Id);
    }

    //根据skuid获取sku最新价格数据
    @GetMapping({"inner/getSkuPrice/{skuId}"})
    public BigDecimal getskuPrice(@PathVariable Long skuId){
        return this.managerService.getSkuPrice(skuId);
    }

    // 根据skuid spuid 获取销售属性及属性值 + 锁定功能
    @GetMapping("/inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable Long skuId,
                                                          @PathVariable Long spuId){
        return this.managerService.getSpuSaleAttrListCheckBySku(skuId, spuId);
    }

    //实现商品切换数据功能
    @GetMapping("inner/getSkuValueIdsMap/{spuId}")
    public Map getSkuValueIdsMap(@PathVariable Long spuId){
        return this.managerService.getSkuValueIdsMap(spuId);
    }

    //根据spuid获取海报数据集合
    @GetMapping("inner/findSpuPosterBySpuId/{spuId}")
    public List<SpuPoster> findSpuPosterBySpuId(@PathVariable Long spuId){
        return this.managerService.getSpuPosterBySpuId(spuId);
    }

    //根据skuId获取[平台属性和属性值 - > 规格参数
    @GetMapping("inner/getAttrList/{skuId}")
    public List<BaseAttrInfo> getAttrList(@PathVariable Long skuId){
        return this.managerService.getAttrList(skuId);
    }

    //访问商品首页分类数据
    @GetMapping("getBaseCategoryList")
    public Result getBaseCategoryList(){
        List<JSONObject> categoryList = this.managerService.getCategoryList();
        return Result.ok(categoryList);
    }

    @GetMapping("inner/getTrademark/{tmId}")
    public BaseTrademark getTrademark(@PathVariable Long tmId){
        return this.managerService.getTrademarkByTrademarkId(tmId);
    }

}
