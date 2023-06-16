package com.atguigu.gmall.list.service.impl;

import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.list.repository.GoodsRepository;
import com.atguigu.gmall.model.list.Goods;
import com.atguigu.gmall.model.list.SearchAttr;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @ClassName: SearchServiceImpl
 * @author: javaermamba
 * @date: 2023-06-2023/6/16-21:31
 * @Description:
 */
@Service
public class SearchServiceImpl implements SearchService {

    //调用service-product这个微服务查询数据库
    @Qualifier("com.atguigu.gmall.product.client.ProductFeignClient")
    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * 上架的本质: 首先给Goods赋值,然后将Goods保存到es索引库中
     *
     * @param skuId
     */
    @Override
    public void upperGoods(Long skuId) {
        Goods goods = new Goods();

        SkuInfo skuInfo = this.productFeignClient.getSkuInfo(skuId);
        goods.setId(skuId);
        goods.setTitle(skuInfo.getSkuName());
        goods.setDefaultImg(skuInfo.getSkuDefaultImg());
        goods.setPrice(productFeignClient.getskuPrice(skuId).doubleValue());
        goods.setCreateTime(new Date());

        //赋值品牌数据
        BaseTrademark trademark = productFeignClient.getTrademark(skuInfo.getTmId());
        goods.setTmId(trademark.getId());
        goods.setTmName(trademark.getTmName());
        goods.setTmLogoUrl(trademark.getLogoUrl());

        //赋值分类属性
        BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
        goods.setCategory1Id(categoryView.getCategory1Id());
        goods.setCategory2Id(categoryView.getCategory2Id());
        goods.setCategory3Id(categoryView.getCategory3Id());

        goods.setCategory1Name(categoryView.getCategory1Name());
        goods.setCategory2Name(categoryView.getCategory2Name());
        goods.setCategory3Name(categoryView.getCategory3Name());

        //商品热度初始化值 都是0

        // 赋值平台属性和属性值 private List<SearchAttr> attrs;
//        ArrayList<SearchAttr> attrs = new ArrayList<>();
//        List<BaseAttrInfo> attrList = this.productFeignClient.getAttrList(skuId);
//        attrList.forEach(attr ->{
//            SearchAttr searchAttr = new SearchAttr();
//            searchAttr.setAttrId(attr.getId());
//            searchAttr.setAttrName(attr.getAttrName());
//            searchAttr.setAttrValue(attr.getAttrValueList().get(0).getValueName());
//            attrs.add(searchAttr);
//        });
        List<BaseAttrInfo> attrsList = this.productFeignClient.getAttrList(skuId);
        List<SearchAttr> attrs = attrsList.stream().map(baseAttrInfo -> {
            SearchAttr searchAttr = new SearchAttr();
            searchAttr.setAttrId(baseAttrInfo.getId());
            searchAttr.setAttrName(baseAttrInfo.getAttrName());
            searchAttr.setAttrValue(baseAttrInfo.getAttrValueList().get(0).getValueName());
            return searchAttr;
        }).collect(Collectors.toList());
        goods.setAttrs(attrs);

        //将goods保存到es中
        this.goodsRepository.save(goods);
    }

    @Override
    public void lowerGoods(Long skuId) {
        this.goodsRepository.deleteById(skuId);
    }

    @Override
    public void incrHotScore(Long skuId) {
        //本质是更新es中hotScore字段的数据 每访问一次,更新一次.这样会伤害性能,每次更新都有io
        //借助redis 记录当前这个商品被访问的次数,如果访问次数达到一定数量,再去更新
        String hotKey = "hotScore";
        Double count = this.redisTemplate.opsForZSet().incrementScore(hotKey, "skuId:" + skuId, 1);
        if (count % 10 == 0) {
            //更新es
            Optional<Goods> optional = this.goodsRepository.findById(skuId);
            Goods goods = optional.get();
            goods.setHotScore(count.longValue());
            //将更新后的数据放入es
            this.goodsRepository.save(goods);
        }
    }
}
