package com.atguigu.gmall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.HttpClientUtil;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.hazelcast.HazelcastHealthContributorAutoConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName: OrderServiceImpl
 * @author: javaermamba
 * @date: 2023-06-2023/6/19-12:59
 * @Description:
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${ware.url}")
    private String wareUrl;

    @Autowired
    private RabbitService rabbitService;

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

        // 下单保存订单消息时,发送延迟消息.到期不付款处理
        this.rabbitService.sendDelayMsg(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL, MqConst.ROUTING_ORDER_CANCEL, orderInfo.getId(), MqConst.DELAY_TIME);

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
        String result = HttpClientUtil.doGet(wareUrl + "/hasStock?skuId=" + skuId + "&num=" + skuNum);
        // 1有 0没有
        return "1".equals(result);
    }

    @Override
    public IPage<OrderInfo> getOrderInfoPage(Page<OrderInfo> pageModel, String userId) {
//        方式一: 关联查询 select * from order_info oi inner join order_detail od on od.order_id = oi.id where user_id = 1;
//        方式二: 使用mapper 单独查询
        IPage<OrderInfo> orderInfoIPage = orderInfoMapper.selectOrderInfoPageList(pageModel, userId);
        // 给数据库中不存在的字段赋值 orderStatusName
        List<OrderInfo> records = orderInfoIPage.getRecords();
        records.forEach(orderInfo -> {
            // 枚举类中根据中文获取状态方法
            String orderStatus = OrderStatus.getStatusNameByStatus(orderInfo.getOrderStatus());
            orderInfo.setOrderStatusName(orderStatus);
        });

        return orderInfoIPage;
    }

    @Override
    public void execExpireOrder(Long orderId) {
        // 取消订单本质: 更新订单状态和订单进度为 CLOSED   order_status  process_status
        // 后续会有很多根据订单id更新订单状态和进程状态的需求. 因此做一个方法抽离.
        this.updateOrderStatus(orderId, ProcessStatus.CLOSED);

        // 还需将本地交易记录的状态变更为CLOSED  以及 支付宝的交易状态也变更
        this.rabbitService.sendMes(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE,MqConst.ROUTING_PAYMENT_CLOSE,orderId);

    }

    @Override
    public void execExpireOrder(Long orderId, String flag) {
        // 取消订单本质: 更新订单状态和订单进度为 CLOSED   order_status  process_status
        // 后续会有很多根据订单id更新订单状态和进程状态的需求. 因此做一个方法抽离.
        this.updateOrderStatus(orderId, ProcessStatus.CLOSED);

        // 放orderInfo和paymentInfo都有消息的情况才去关闭 将flag设置为2
        if ("2".equals(flag)){
            // 还需将本地交易记录的状态变更为CLOSED  以及 支付宝的交易状态也变更
            this.rabbitService.sendMes(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE,MqConst.ROUTING_PAYMENT_CLOSE,orderId);
        }
    }

    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        OrderInfo orderInfo = this.orderInfoMapper.selectById(orderId);

        if (orderInfo != null) {
            LambdaQueryWrapper<OrderDetail> orderDetailLambdaQueryWrapper = new LambdaQueryWrapper<>();
            orderDetailLambdaQueryWrapper.eq(OrderDetail::getOrderId, orderId);
            List<OrderDetail> orderDetailList = this.orderDetailMapMapper.selectList(orderDetailLambdaQueryWrapper);
            orderInfo.setOrderDetailList(orderDetailList);
        }
        return orderInfo;
    }

    /**
     * 更新订单方法          // 后续会有很多根据订单id更新订单状态和进程状态的需求. 因此做一个方法抽离.
     *
     * @param orderId
     * @param processStatus
     */
    @Override
    public void updateOrderStatus(Long orderId, ProcessStatus processStatus) {
        // 取消订单本质: 更新订单状态和订单进度为 CLOSED   order_status  process_status
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());
        orderInfo.setProcessStatus(processStatus.name());
        this.orderInfoMapper.updateById(orderInfo);
    }

    @Override
    public void sendOrderStatus(Long orderId) {
        // 发送消息给库存系统 减库存
        this.updateOrderStatus(orderId, ProcessStatus.NOTIFIED_WARE);
        //发送消息参数 为库存系统所需JSON格式封装数据
        String wareJson = initWareOrder(orderId);
        this.rabbitService.sendMes(MqConst.EXCHANGE_DIRECT_WARE_STOCK, MqConst.ROUTING_WARE_STOCK, wareJson);
    }

    /**
     * 封装库存消息所需参数
     *
     * @param orderId
     * @return
     */
    private String initWareOrder(Long orderId) {
        // JSON字符串是由orderInfo的部分字段组成
        OrderInfo orderInfo = this.getOrderInfo(orderId);
        // 将orderInfo -> map
        Map map = this.initWareOrder(orderInfo);
        return JSON.toJSONString(map);
    }

    /**
     * 转换orderInfo -> map
     *
     * @param orderInfo
     * @return
     */
    @Override
    public Map initWareOrder(OrderInfo orderInfo) {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("orderId", orderInfo.getId());
        hashMap.put("consignee", orderInfo.getConsignee());
        hashMap.put("consigneeTel", orderInfo.getConsigneeTel());
        hashMap.put("orderComment", orderInfo.getOrderComment());
        hashMap.put("orderBody", orderInfo.getTradeBody());
        hashMap.put("deliverAddress", orderInfo.getDeliveryAddress());
        hashMap.put("paymentWay", 2);

        hashMap.put("wareId", orderInfo.getWareId());

        // 上面封装orderInfo的 下面 details明细
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
//        ArrayList<Map> maps = new ArrayList<>();
//        orderDetailList.forEach(orderDetail -> {
//            HashMap<String, Object> detailMap = new HashMap<>();
//            detailMap.put("skuId",orderDetail.getSkuId());
//            detailMap.put("skuNum",orderDetail.getSkuNum());
//            detailMap.put("skuName",orderDetail.getSkuName());
//            maps.add(detailMap);
//        });

        List<HashMap<String, Object>> detailListMap = orderDetailList.stream().map(orderDetail -> {
            HashMap<String, Object> detailMap = new HashMap<>();
            detailMap.put("skuId", orderDetail.getSkuId());
            detailMap.put("skuNum", orderDetail.getSkuNum());
            detailMap.put("skuName", orderDetail.getSkuName());
            return detailMap;
        }).collect(Collectors.toList());

        hashMap.put("details", detailListMap);
        return hashMap;
    }

    @Override
    public List<OrderInfo> orderSplit(String orderId, String wareSkuMap) {
        /*
                拆单业务:
                    1. 现获取原始订单
                    2. 将wareSkuMap的JSON格式数据 变为 能操作的对象
                    3. 创建新的子订单,并赋值
                    4. 子订单添加到子订单集合
                    5. 保存新的子订单
                    6. 更新原始订单状态
         */

        // 4 创建子订单集合对象
        ArrayList<OrderInfo> subOrderDetailList = new ArrayList<>();

        OrderInfo orderInfoOrigin = this.getOrderInfo(Long.parseLong(orderId));
        // 2
        List<Map> mapList = JSON.parseArray(wareSkuMap, Map.class);
        for (Map map : mapList) {
            String warwId = (String) map.get("warwId");
            // 仓库下可能会有很多商品数据
            List<String> skuIdList = (List<String>) map.get("skuIds");
            // 3
            OrderInfo subOrderInfo = new OrderInfo();
            BeanUtils.copyProperties(orderInfoOrigin, subOrderInfo);
            // 主键自增
            subOrderInfo.setId(null);
            // 自订单服id
            subOrderInfo.setParentOrderId(Long.parseLong(orderId));
            // 赋值一个仓库id
            subOrderInfo.setWareId(warwId);

            // 声明子订单明细
            ArrayList<OrderDetail> subOrderDetails = new ArrayList<>();
            // 重新计算子订单金额  单价*数量 方法需要子订单明细数据支撑
            List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
            for (OrderDetail orderDetail : orderDetailList) {
                // 循环JSON 字符串中获取到对应的skuId
                for (String skuId : skuIdList) {
                    if (orderDetail.getSkuId().longValue() == Long.parseLong(skuId)) {
                        // 找到了订单明细
                        subOrderDetails.add(orderDetail);
                    }
                }
            }
            subOrderInfo.setOrderDetailList(subOrderDetails);
            subOrderInfo.sumTotalAmount();

            subOrderDetailList.add(subOrderInfo);

            // 5 子订单保存到数据库
            this.saveOrderInfo(subOrderInfo);
        }

        // 6 将原始订单状态 变为 SPLIT
        this.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.SPLIT);

        return subOrderDetailList;
    }
}
