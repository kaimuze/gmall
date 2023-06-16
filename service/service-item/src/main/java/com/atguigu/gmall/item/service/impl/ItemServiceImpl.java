package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.model.item.ItemVo;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.jws.HandlerChain;
import javax.jws.Oneway;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @ClassName: ItemServiceImpl
 * @author: javaermamba
 * @date: 2023-06-2023/6/15-14:44
 * @Description:
 */
@Service
public class ItemServiceImpl implements ItemService {

    // item汇总数据 product负责查询数据库 这里需要调用product模块
    @Qualifier("com.atguigu.gmall.product.client.ProductFeignClient")
    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public ItemVo getItem(Long skuId) {
        //数据汇总

        ItemVo itemVo = new ItemVo();

        // 判断布隆过滤器是否包含数据  在商品详情处做判断,是否继续向下查询
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConst.SKU_BLOOM_FILTER);
        if (!bloomFilter.contains(skuId)) {
            return itemVo;
        }

        // 1 skuinfo
        SkuInfo skuInfo = this.productFeignClient.getSkuInfo(skuId);
        // 2 获取分类数据
        BaseCategoryView categoryView = this.productFeignClient.getCategoryView(skuInfo.getCategory3Id());

        //3 价格信息
        BigDecimal skuPrice = this.productFeignClient.getskuPrice(skuId);

        //4 获取销售属性和属性值 锁定
        List<SpuSaleAttr> spuSaleAttrList = this.productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());

        //5 切换功能数据
        Map skuValueIdsMap = this.productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
        String strJson = JSON.toJSONString(skuValueIdsMap);

        //6 获取商品海报
        List<SpuPoster> spuPosterList = this.productFeignClient.findSpuPosterBySpuId(skuInfo.getSpuId());

        //7 商品规格参数
        List<BaseAttrInfo> attrList = this.productFeignClient.getAttrList(skuId);

        itemVo.setSkuInfo(skuInfo);
        itemVo.setCategoryView(categoryView);
        itemVo.setSkuPrice(skuPrice);
        itemVo.setSpuSaleAttrList(spuSaleAttrList);
        itemVo.setValuesSkuJson(strJson);
        itemVo.setSpuPosterList(spuPosterList);
        //     private List<Map<String, String>> skuAttrList;  List<BaseAttrInfo> attrList 格式不符
        List<Map<String, String>> attrMapList = attrList.stream().map(baseAttrInfo -> {
            Map<String, String> hashMap = new HashMap<>();
            String attrName = baseAttrInfo.getAttrName();
            hashMap.put("attrName", attrName);
            String valueName = baseAttrInfo.getAttrValueList().get(0).getValueName();
            hashMap.put("attrValue", valueName);
            return hashMap;
        }).collect(Collectors.toList());
        itemVo.setSkuAttrList(attrMapList);
        return itemVo;
    }
}
