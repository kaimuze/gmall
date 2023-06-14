package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.atguigu.gmall.product.mapper.BaseCategoryTrademarkMapper;
import com.atguigu.gmall.product.mapper.BaseTrademarkMapper;
import com.atguigu.gmall.product.service.BaseCategoryTrademarkService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName: BaseCategoryTrademarkServiceImpl
 * @author: javaermamba
 * @date: 2023-06-2023/6/14-11:26
 * @Description:
 */
@Service
public class BaseCategoryTrademarkServiceImpl extends ServiceImpl<BaseCategoryTrademarkMapper,BaseCategoryTrademark> implements BaseCategoryTrademarkService {

    @Autowired
    private BaseCategoryTrademarkMapper baseCategoryTrademarkMapper;

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    @Override
    public List<BaseTrademark> getTrademarkList(Long category3Id) {
        // 参数只有category3id 只能现获取到base_category_trademark表中的trademark_id 再查询相应的品牌信息数据
        // select * from base_category_trademark where category3_id = ?;
        LambdaQueryWrapper<BaseCategoryTrademark> baseCategoryTrademarkLambdaQueryWrapper = new LambdaQueryWrapper<>();
        baseCategoryTrademarkLambdaQueryWrapper.eq(BaseCategoryTrademark::getCategory3Id,category3Id);
        List<BaseCategoryTrademark> baseCategoryTrademarkList = this.baseCategoryTrademarkMapper.selectList(baseCategoryTrademarkLambdaQueryWrapper);
        // 匹配base_trademark中的id
//        ArrayList<Long> idList = new ArrayList<>();
//        baseCategoryTrademarkList.forEach(baseCategoryTrademark -> {
//            idList.add(baseCategoryTrademark.getTrademarkId());
//        });
        if (!CollectionUtils.isEmpty(baseCategoryTrademarkList)){
            List<Long> idList = baseCategoryTrademarkList.stream().map(BaseCategoryTrademark::getTrademarkId).collect(Collectors.toList());

            //根据idList获取到base_trademark集合数据
//            ArrayList<BaseTrademark> baseTrademarks = new ArrayList<>();
//            idList.stream().forEach(item ->{
//                BaseTrademark baseTrademark = baseTrademarkMapper.selectById(item);
//                baseTrademarks.add(baseTrademark);
//            });
            return baseTrademarkMapper.selectBatchIds(idList);
        }
        //默认返回空
        return null;
    }

    @Override
    public List<BaseTrademark> getCurrentCategoryTrademarkList(Long category3Id) {
        // 获取两个集合的差集    即为可选品牌列表
        // 2 获取已经存在绑定的品牌集合数据
        LambdaQueryWrapper<BaseCategoryTrademark> baseCategoryTrademarkLambdaQueryWrapper = new LambdaQueryWrapper<>();
        baseCategoryTrademarkLambdaQueryWrapper.eq(BaseCategoryTrademark::getCategory3Id,category3Id);
        List<BaseCategoryTrademark> baseCategoryTrademarkList = this.baseCategoryTrademarkMapper.selectList(baseCategoryTrademarkLambdaQueryWrapper);
        if (!CollectionUtils.isEmpty(baseCategoryTrademarkList)){
            List<Long> idList = baseCategoryTrademarkList.stream().map(BaseCategoryTrademark::getTrademarkId).collect(Collectors.toList());
            // 1 获取所有品牌数据集合
            List<BaseTrademark> baseTrademarkList = this.baseTrademarkMapper.selectList(null);
            // 3 通过id取差集
            List<BaseTrademark> baseTrademarks = baseTrademarkList.stream().filter(baseTrademark -> {
                return !idList.contains(baseTrademark.getId());
            }).collect(Collectors.toList());
            return baseTrademarks;
        }
        // 没有绑定的品牌情况下,查询所有品牌集合数据
        return this.baseTrademarkMapper.selectList(null);
    }

    @Override
    public void save(CategoryTrademarkVo categoryTrademarkVo) {
        // {category3Id: 61, trademarkIdList: [9]}
        // insert into base_category_trademark
//        BaseCategoryTrademark baseCategoryTrademark = new BaseCategoryTrademark();
//        baseCategoryTrademark.setCategory3Id(categoryTrademarkVo.getCategory3Id());
//        List<Long> trademarkIdList = categoryTrademarkVo.getTrademarkIdList();
//        trademarkIdList.forEach(baseCategoryTrademark::setTrademarkId);
//        this.baseCategoryTrademarkMapper.insert(baseCategoryTrademark);

        //方式二: 批量插入数据 接口实现IService接口 实现类继承ServiceImpl类 调用saveBatch方法
        List<BaseCategoryTrademark> baseCategoryTrademarkList = categoryTrademarkVo.getTrademarkIdList().stream().map(trademarkId -> {
            BaseCategoryTrademark baseCategoryTrademark1 = new BaseCategoryTrademark();
            baseCategoryTrademark1.setCategory3Id(categoryTrademarkVo.getCategory3Id());
            baseCategoryTrademark1.setTrademarkId(trademarkId);
            return baseCategoryTrademark1;
        }).collect(Collectors.toList());
        this.saveBatch(baseCategoryTrademarkList);
    }

    @Override
    public void removeTrademark(Long category3Id, Long trademarkId) {
        LambdaQueryWrapper<BaseCategoryTrademark> baseCategoryTrademarkLambdaQueryWrapper = new LambdaQueryWrapper<>();
        baseCategoryTrademarkLambdaQueryWrapper.eq(BaseCategoryTrademark::getCategory3Id,category3Id);
        baseCategoryTrademarkLambdaQueryWrapper.eq(BaseCategoryTrademark::getTrademarkId,trademarkId);
        this.baseCategoryTrademarkMapper.delete(baseCategoryTrademarkLambdaQueryWrapper);
    }
}
