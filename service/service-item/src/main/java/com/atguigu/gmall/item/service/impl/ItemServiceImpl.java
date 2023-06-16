package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.model.item.ItemVo;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
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

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public ItemVo getItem(Long skuId) {
        //数据汇总

        ItemVo itemVo = new ItemVo();

        // 判断布隆过滤器是否包含数据  在商品详情处做判断,是否继续向下查询
        // *注意: 这里注释掉为方便测试,若开启布隆过滤器的话会把之前的测试数据拦截掉
//        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConst.SKU_BLOOM_FILTER);
//        if (!bloomFilter.contains(skuId)) {
//            return itemVo;
//        }

        // 使用CompletableFuture实现商品详情数据汇总的多任务组合优化 - 异步编排

        CompletableFuture<SkuInfo> skuInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            // 1 skuinfo
            SkuInfo skuInfo = this.productFeignClient.getSkuInfo(skuId);
            itemVo.setSkuInfo(skuInfo);
            return skuInfo;
        },threadPoolExecutor);

        CompletableFuture<Void> categoryViewCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            // 2 获取分类数据
            BaseCategoryView categoryView = this.productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            itemVo.setCategoryView(categoryView);
        },threadPoolExecutor);

        CompletableFuture<Void> skuPriceCompletableFuture = CompletableFuture.runAsync(() -> {
            //3 价格信息
            BigDecimal skuPrice = this.productFeignClient.getskuPrice(skuId);
            itemVo.setSkuPrice(skuPrice);
        },threadPoolExecutor);

        CompletableFuture<Void> spuSaleAttrCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            //4 获取销售属性和属性值 锁定
            List<SpuSaleAttr> spuSaleAttrList = this.productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
            itemVo.setSpuSaleAttrList(spuSaleAttrList);
        },threadPoolExecutor);

        CompletableFuture<Void> strCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            //5 切换功能数据
            Map skuValueIdsMap = this.productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
            String strJson = JSON.toJSONString(skuValueIdsMap);
            itemVo.setValuesSkuJson(strJson);
        },threadPoolExecutor);

        CompletableFuture<Void> spuPosterCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            //6 获取商品海报
            List<SpuPoster> spuPosterList = this.productFeignClient.findSpuPosterBySpuId(skuInfo.getSpuId());
            itemVo.setSpuPosterList(spuPosterList);
        },threadPoolExecutor);

        CompletableFuture<Void> attrCompletableFuture = CompletableFuture.runAsync(() -> {
            //7 商品规格参数
            List<BaseAttrInfo> attrList = this.productFeignClient.getAttrList(skuId);

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
        },threadPoolExecutor);

        // 多任务组合 汇集所有远程调用的线程进行数据汇总
        CompletableFuture.allOf(
                skuInfoCompletableFuture,
                categoryViewCompletableFuture,
                skuPriceCompletableFuture,
                spuSaleAttrCompletableFuture,
                spuPosterCompletableFuture,
                strCompletableFuture,
                attrCompletableFuture).join();
        //释放资源
//        threadPoolExecutor.shutdown();
        // 返回数据
        return itemVo;
    }
}
