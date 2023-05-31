package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManagerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

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
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

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
        return baseAttrInfoMapper.selectAttrInfoList(category1Id,category2Id,category3Id);
    }

    /**
     * 保存平台属性信息数据
     * 注意: 平台属性和平台属性值是一对一关系,保存平台属性信息的同时也要保存平台属性值的对应属性信息
     * 操作两张表,需要事务保证原子性
     * 注意: 必须是要rollbackFor,且异常类型时exception类型,因为不加的话,默认是运行时异常runtimeException
     * @param baseAttrInfo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {

        //既有保存又有修改实现  有id是修改 没id是保存插入
        //1.先回显getAttrValueList  2.再修改保存
        // 问题分析: 在用户修改的时候,我们不能确定用户是要对这组数据进行的具体增删改操作怎么办,所以在此修改不能使用update语句. 直接先删除再新增!
        // 2.通过id表示区分是修改还是保存
        if (baseAttrInfo.getId() == null){
            //平台属性id值为空就是保存添加
            //保存数据
            // base_attr_info  base_attr_value 操作这两张表
            //针对base_attr_info表的保存操作
            baseAttrInfoMapper.insert(baseAttrInfo);
        }else{
            //平台属性id值不为空则是修改
            // 对于base_attr_info表的修改操作
            baseAttrInfoMapper.updateById(baseAttrInfo);

            //针对base_attr_value表的操作
            // 1. 先删除表中数据,再添加新增数据
            LambdaQueryWrapper<BaseAttrValue> baseAttrValueLambdaQueryWrapper = new LambdaQueryWrapper<>();
            baseAttrValueLambdaQueryWrapper.eq(BaseAttrValue::getAttrId,baseAttrInfo.getId());
            baseAttrValueMapper.delete(baseAttrValueLambdaQueryWrapper);
        }

        //2/ 再添加,新增数据
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        if (!CollectionUtils.isEmpty(attrValueList)){
            attrValueList.forEach(baseAttrValue -> {
                // 注意: base_attr_value.id = base_attr_info.id 先执行base_attr_info表的insert语句,base_attr_info.id通过主键回填获取
                baseAttrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insert(baseAttrValue);
            });
        }
    }

    /**
     * 根据平台属性id查询平台属性值数据集合:回显
     * @param attrId
     * @return
     */
    @Override
    public List<BaseAttrValue> getAttrValueList(Long attrId) {
        LambdaQueryWrapper<BaseAttrValue> baseAttrValueLambdaQueryWrapper = new LambdaQueryWrapper<>();
        baseAttrValueLambdaQueryWrapper.eq(BaseAttrValue::getAttrId,attrId);
        return this.baseAttrValueMapper.selectList(baseAttrValueLambdaQueryWrapper);
    }

    @Override
    public BaseAttrInfo getBaseAttrInfo(Long attrId) {
        LambdaQueryWrapper<BaseAttrInfo> baseAttrInfoLambdaQueryWrapper = new LambdaQueryWrapper<>();
        baseAttrInfoLambdaQueryWrapper.eq(BaseAttrInfo::getId,attrId);
//        return this.baseAttrInfoMapper.selectOne(baseAttrInfoLambdaQueryWrapper);
        //注意这里为什么不能直接返回查询到的对象??
        //注意前端控制器中返回的数据是baseAttrValueList数据,我们这里仅仅是查询到了base_attr_info表中的数据,并没有封装BaseAttrValue数据
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectById(attrId);
        if (baseAttrInfo != null) {
            baseAttrInfo.setAttrValueList(getAttrValueList(attrId));
        }
        return baseAttrInfo;
    }
}
