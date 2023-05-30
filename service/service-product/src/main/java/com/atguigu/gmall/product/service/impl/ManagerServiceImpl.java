package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManagerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @ClassName: ManagerServiceImpl
 * @author: javaermamba
 * @date: 2023-05-2023/5/25-14:57
 * @Description:
 */
@Service
public class ManagerServiceImpl implements ManagerService {

    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private com.atguigu.gmall.product.mapper.baseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttributeInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttributeValueMapper;

    @Override
    public List<BaseCategory1> getCategory1() {
        // select * from base_category1;
        return baseCategory1Mapper.selectList(null);
    }

    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {
        LambdaQueryWrapper<BaseCategory2> baseCategory2LambdaQueryWrapper = new LambdaQueryWrapper<>();
        baseCategory2LambdaQueryWrapper.eq(BaseCategory2::getCategory1Id,category1Id);
        return baseCategory2Mapper.selectList(baseCategory2LambdaQueryWrapper);
    }

    @Override
    public List<BaseCategory3> getCategory3(Long category2Id) {
        LambdaQueryWrapper<BaseCategory3> baseCategory3LambdaQueryWrapper = new LambdaQueryWrapper<>();
        baseCategory3LambdaQueryWrapper.eq(BaseCategory3::getCategory2Id,category2Id);
        return this.baseCategory3Mapper.selectList(baseCategory3LambdaQueryWrapper);
    }

    @Override
    public List<BaseAttrInfo> getAttrInfoListByCategoryId(Long category1Id, Long category2Id, Long category3Id) {
        //根据分类id获取平台属性数据  集合数据
/*
        select
               bai.id,
               bai.attr_name,
               bai.category_id,
               bai.category_level,
               bai.id value_id,
               bav.value_name
        from base_attr_info bai
                 inner join base_attr_value bav
                            on bai.id = bav.attr_id
        where (bai.category_id = ?
            and bai.category_level = 1)
           or (bai.category_id = ?
            and bai.category_level = 2)
           or (bai.category_id = ?
            and bai.category_level = 3)
        order by bai.category_level, bai.id;
*/
        return baseAttributeInfoMapper.selectAttrInfoList(category1Id,category2Id,category3Id);
    }
}
