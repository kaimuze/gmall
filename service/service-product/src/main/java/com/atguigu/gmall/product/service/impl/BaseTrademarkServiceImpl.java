package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.mapper.BaseTrademarkMapper;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @ClassName: BaseTrademarkServiceImpl
 * @author: javaermamba
 * @date: 2023-06-2023/6/13-22:11
 * @Description:
 */
@Service
public class BaseTrademarkServiceImpl extends ServiceImpl<BaseTrademarkMapper, BaseTrademark> implements BaseTrademarkService {

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    @Override
    public void removeTrademarkById(Long id) {
        baseTrademarkMapper.deleteById(id);
    }

    @Override
    public void saveTrademark(BaseTrademark baseTrademark) {
        baseTrademarkMapper.insert(baseTrademark);
    }
}
