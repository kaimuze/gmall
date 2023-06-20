package com.atguigu.gmall.payment.service.impl;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.aspectj.runtime.internal.cflowstack.ThreadStackImpl11;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

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

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitService rabbitService;

    @Override
    public void savePaymentInfo(OrderInfo orderInfo, String paymentType) {

        //交易记录表 一个订单同一种支付方式,不能存在相同的数据,不允许.一个订单de订单id和支付类型不能有多条一样的
        LambdaQueryWrapper<PaymentInfo> paymentInfoLambdaQueryWrapper = new LambdaQueryWrapper<>();
        paymentInfoLambdaQueryWrapper.eq(PaymentInfo::getOrderId, orderInfo.getId());
        paymentInfoLambdaQueryWrapper.eq(PaymentInfo::getPaymentType, paymentType);
        PaymentInfo paymentInfoQuery = this.paymentInfoMapper.selectOne(paymentInfoLambdaQueryWrapper);
        //如果有当前订单以及支付方式的记录,则返回,否则插入信息
        if (paymentInfoQuery != null) {
            return;
        }

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setOrderId(orderInfo.getId());
        paymentInfo.setUserId(orderInfo.getUserId());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        // callback_time callback_content 异步回调时再给值
        this.paymentInfoMapper.insert(paymentInfo);
    }

    // 根据outTradeNo  paymentType 获取交易记录状态
    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo, String paymentType) {
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no", outTradeNo);
        paymentInfoQueryWrapper.eq("payment_type", paymentType);
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(paymentInfoQueryWrapper);
        if (paymentInfo != null) {
            return paymentInfo;
        }
        return null;
    }

    // 支付成功,更新记录
    @Override
    public void paySuccess(String outTradeNo, String paymentType, Map<String, String> paramMap) {
        // 先查询当前是否有对应的交易记录
        PaymentInfo paymentInfoQuery = this.getPaymentInfo(outTradeNo, paymentType);
        if (paymentInfoQuery== null){
            return;
        }
        //更新数据
        try {
//            UpdateWrapper<PaymentInfo> paymentInfoUpdateWrapper = new UpdateWrapper<>();
//            paymentInfoUpdateWrapper.eq("out_trade_no",outTradeNo);
//            paymentInfoUpdateWrapper.eq("payment_type",paymentType);
            PaymentInfo paymentInfo = new PaymentInfo();
//        paymentInfo.setOutTradeNo(outTradeNo);
//        paymentInfo.setPaymentType(paymentType);
            //更新支付宝的交易号
            paymentInfo.setTradeNo(paramMap.get("trade_no"));
            paymentInfo.setCallbackTime(new Date());
            paymentInfo.setCallbackContent(paramMap.toString());
            paymentInfo.setPaymentStatus(PaymentStatus.PAID.name());
            //this.paymentInfoMapper.update(paymentInfo,paymentInfoUpdateWrapper);

            // 更新订单方法的抽离
            this.updatePaymentInfo(outTradeNo,paymentType,paymentInfo);

        } catch (Exception e) {
            // 由于网络抖动出现了异常....
            // 这里捕获到异常,删除key ,以便与再一次验证请求过来,可以再次重新执行更新操作
            this.redisTemplate.delete(paramMap.get("notify_id"));
            e.printStackTrace();
        }

        // 支付成功发送消息给订单,通知订单更新状态  发送订单id
        this.rabbitService.sendMes(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,MqConst.ROUTING_PAYMENT_PAY,paymentInfoQuery.getOrderId());

    }

    // 更新交易记录
    @Override
    public void updatePaymentInfo(String outTradeNo, String paymentType, PaymentInfo paymentInfo) {
        UpdateWrapper<PaymentInfo> paymentInfoUpdateWrapper = new UpdateWrapper<>();
        paymentInfoUpdateWrapper.eq("out_trade_no",outTradeNo);
        paymentInfoUpdateWrapper.eq("payment_type",paymentType);
        paymentInfoMapper.update(paymentInfo,paymentInfoUpdateWrapper);
    }

    @Override
    public void closePayment(Long orderId) {
        LambdaQueryWrapper<PaymentInfo> paymentInfoLambdaQueryWrapper = new LambdaQueryWrapper<>();
        paymentInfoLambdaQueryWrapper.eq(PaymentInfo::getOrderId,orderId);
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.CLOSED.name());
        paymentInfo.setUpdateTime(new Date());
        this.paymentInfoMapper.update(paymentInfo,paymentInfoLambdaQueryWrapper);
    }


}
