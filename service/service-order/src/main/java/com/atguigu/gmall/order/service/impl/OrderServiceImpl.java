package com.atguigu.gmall.order.service.impl;

import com.atguigu.gmall.common.util.HttpClientUtil;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @ClassName: OrderServiceImpl
 * @author: javaermamba
 * @date: 2023-06-2023/6/19-12:59
 * @Description:
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${ware.url}")
    private String wareUrl;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveOrderInfo(OrderInfo orderInfo) {
        // 明确向哪些表保存数据  order_info order_detail

        //看页面哪些数据没有传递 手动封装
        // total_amount order_status  user_id out_trade_no trade_body operate_time expire_time process_status

        // 单价*数量
        // orderInfo.sumTotalAmount()  必须保证订单详情列表集合有数据
        orderInfo.sumTotalAmount();

        //订单状态  初始值:未付款
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());

        //第三方交易编号 保证支付的幂等性(订单不能重复提交)
        String outTradeNo = "ATGUIGU" + new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);

        //订单描述  title:固定值
        orderInfo.setTradeBody("基尼太美,感谢消费");

        //操作时间
        orderInfo.setOperateTime(new Date());

        //过期时间 每个商品对应的订单过期时间可能不同  这里统一一天
        Calendar instance = Calendar.getInstance();
        //当前系统时间+1天
        instance.add(Calendar.DATE, 1);
        orderInfo.setExpireTime(instance.getTime());

        //订单状态
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());

        // order_info
        orderInfoMapper.insert(orderInfo);

        // order_detail
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        orderDetailList.forEach(orderDetail -> {
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapMapper.insert(orderDetail);
        });

        return orderInfo.getId();
    }

    @Override
    public String getTradeNo(String userId) {
        String tradeNo = UUID.randomUUID().toString();

        String tradeNoKey = "tradeNo:" + userId;
        this.redisTemplate.opsForValue().set(tradeNoKey, tradeNo);

        return tradeNo;
    }

    /**
     * @param userId  组成缓存key
     * @param tradeNo 前端传递过来的流水号
     * @return
     */
    @Override
    public Boolean checkTradeNo(String userId, String tradeNo) {
        String tradeNoKey = "tradeNo:" + userId;

        // 获取缓存中的数据
        String tradeNoRedis = (String) this.redisTemplate.opsForValue().get(tradeNoKey);
        //返回比较结果
        return tradeNo.equals(tradeNoRedis);
    }

    @Override
    public void delTradeNo(String userId) {
        String tradeNoKey = "tradeNo:" + userId;
        this.redisTemplate.delete(tradeNoKey);
    }

    // 远程调用库粗系统 http://localhost:9001/hasStock?skuid=1022&num=2  库存系统SpringBoot框架,无法使用feign调用,使用HttpClient调用
    @Override
    public Boolean checkStock(Long skuId, Integer skuNum) {
        //ware:
        //  url: http://localhost:9001   HttpClient调用
        String result = HttpClientUtil.doGet(wareUrl + "/hasStock?skuid=" + skuId + "&num=" + skuNum);
        // 1有 0没有
        return "1".equals(result);
    }
}
