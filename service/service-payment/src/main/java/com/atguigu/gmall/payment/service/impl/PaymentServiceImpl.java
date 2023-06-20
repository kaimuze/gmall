package com.atguigu.gmall.payment.service.impl;

import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @ClassName: PaymentServiceImpl
 * @Project: gmall-parent
 * @Package: com.atguigu.gmall.payment.service.impl
 * @Author: carry.kaimuze
 * @Date: 2023-2023/6/20-18:43
 * @Description:
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Override
    public void savePaymentInfo(OrderInfo orderInfo, String paymentType) {

        //交易记录表 一个订单同一种支付方式,不能存在相同的数据,不允许.一个订单de订单id和支付类型不能有多条一样的
        LambdaQueryWrapper<PaymentInfo> paymentInfoLambdaQueryWrapper = new LambdaQueryWrapper<>();
        paymentInfoLambdaQueryWrapper.eq(PaymentInfo::getOrderId,orderInfo.getId());
        paymentInfoLambdaQueryWrapper.eq(PaymentInfo::getPaymentType,paymentType);
        PaymentInfo paymentInfoQuery = this.paymentInfoMapper.selectOne(paymentInfoLambdaQueryWrapper);
        //如果有当前订单以及支付方式的记录,则返回,否则插入信息
        if (paymentInfoQuery!=null) {
            return;
        }

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setOrderId(orderInfo.getId());
//        paymentInfo.setUserId();控制器赋值
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
        paymentInfo.setPaymentType(PaymentType.ALIPAY.name());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        // callback_time callback_content 异步回调时再给值
        this.paymentInfoMapper.insert(paymentInfo);
    }
}
