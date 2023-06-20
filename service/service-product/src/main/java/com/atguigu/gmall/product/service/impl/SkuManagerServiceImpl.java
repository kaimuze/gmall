package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.product.SkuAttrValue;
import com.atguigu.gmall.model.product.SkuImage;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SkuSaleAttrValue;
import com.atguigu.gmall.product.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.product.mapper.SkuImageMapper;
import com.atguigu.gmall.product.mapper.SkuInfoMapper;
import com.atguigu.gmall.product.mapper.SkuSaleAttrValueMapper;
import com.atguigu.gmall.product.service.SkuManagerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @ClassName: SkuManagerServiceImpl
 * @author: javaermamba
 * @date: 2023-06-2023/6/15-11:11
 * @Description:
 */
@Service
public class SkuManagerServiceImpl implements SkuManagerService {

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RabbitService rabbitService;

    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {
        // sku_info sku_image sku_attr_value sku_sale_attr_value
        skuInfoMapper.insert(skuInfo);

        // sku_image
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (!CollectionUtils.isEmpty(skuImageList)){
            skuImageList.forEach(skuImage -> {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insert(skuImage);
            });
        }

        // sku_sale_attr_value
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (!CollectionUtils.isEmpty(skuSaleAttrValueList)){
            skuSaleAttrValueList.forEach(skuSaleAttrValue -> {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValue.setSpuId(skuInfo.getSpuId());
                skuSaleAttrValueMapper.insert(skuSaleAttrValue);
            });
        }

        // sku_attr_value
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (!CollectionUtils.isEmpty(skuAttrValueList)) {
            skuAttrValueList.forEach(skuAttrValue -> {
                skuAttrValue.setSkuId(skuInfo.getId());
                this.skuAttrValueMapper.insert(skuAttrValue);
            });
        }

        //在添加sku的时候,就将skuid添加到布隆过滤器中,为之后的查询请求提供帮助(例如在商品详情信息汇总处)
        RBloomFilter<Object> bloomFilter = this.redissonClient.getBloomFilter(RedisConst.SKU_BLOOM_FILTER);
        bloomFilter.add(skuInfo.getId());
    }

    @Override
    public IPage<SkuInfo> getListSku(Page<SkuInfo> pageModel, Long category3Id) {
        LambdaQueryWrapper<SkuInfo> skuInfoLambdaQueryWrapper = new LambdaQueryWrapper<>();
        skuInfoLambdaQueryWrapper.eq(SkuInfo::getCategory3Id,category3Id);
        skuInfoLambdaQueryWrapper.orderByDesc(SkuInfo::getId);
        return this.skuInfoMapper.selectPage(pageModel,skuInfoLambdaQueryWrapper);
    }

    //后台管理系统的商品上架操作  优化:通过RabbitMQ与es的上下架关联,实现一键上架下架商品
    @Override
    public void onSale(Long skuId) {
        // is_sale = 1
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(1);
        this.skuInfoMapper.updateById(skuInfo);

        // 发送消息到队列给消费者
        // 消息主体: 根据消费者的需求填写  list模块作为消费者,上架操作需要skuId
        this.rabbitService.sendMes(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_UPPER,skuId);

    }

    @Override
    public void cancelSale(Long skuId) {
        // is_sale = 0
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(0);
        this.skuInfoMapper.updateById(skuInfo);

        // 发送消息到队列给消费者
        // 消息主体: 根据消费者的需求填写  list模块作为消费者,上架操作需要skuId
        this.rabbitService.sendMes(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_LOWER,skuId);

    }
}
