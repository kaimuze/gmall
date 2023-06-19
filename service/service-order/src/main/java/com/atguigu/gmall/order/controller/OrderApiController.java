package com.atguigu.gmall.order.controller;

import com.atguigu.gmall.cart.client.ServiceCartFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @ClassName: OrderController
 * @author: javaermamba
 * @date: 2023-06-2023/6/19-11:05
 * @Description:
 */
@RestController
@RequestMapping("/api/order")
public class OrderApiController {

    @Qualifier("com.atguigu.gmall.cart.client.ServiceCartFeignClient")
    @Autowired
    private ServiceCartFeignClient serviceCartFeignClient;

    @Qualifier("com.atguigu.gmall.user.client.UserFeignClient")
    @Autowired
    private UserFeignClient userFeignClient;

    @Qualifier("com.atguigu.gmall.product.client.ProductFeignClient")
    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private OrderService orderService;

    //结算
    @GetMapping("auth/trade")
    public Result authTrade(HttpServletRequest request){

        String userId = AuthContextHolder.getUserId(request);
        //收货地址列表
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(Long.parseLong(userId));

        //送货清单
        List<CartInfo> cartCheckedList = this.serviceCartFeignClient.getCartCheckedList(Long.parseLong(userId));
        // CartInfo => OrderDetail
        AtomicInteger totalNum = new AtomicInteger();
        List<OrderDetail> orderDetailList = cartCheckedList.stream().map(cartInfo -> {
            OrderDetail orderDetail = new OrderDetail();
            // 页面没有渲染skuId但是要给到,以备使用
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setSkuName(cartInfo.getSkuName());
            // cartInfo.getSkuPrice()加入购物车价格    cartInfo.getSkuPrice()实时价格
            orderDetail.setOrderPrice(cartInfo.getSkuPrice());

            // 计算总件数
            totalNum.addAndGet(cartInfo.getSkuNum());


            return orderDetail;
        }).collect(Collectors.toList());

        // 商品总金额
        OrderInfo orderInfo = new OrderInfo();
        //赋值订单明细结果
        orderInfo.setOrderDetailList(orderDetailList);
        orderInfo.sumTotalAmount();


        //-----------------------------测试方法用------------------------------------------
        // totalNum 总件数
        int sum = cartCheckedList.stream().mapToInt(CartInfo::getSkuNum).sum();
        System.out.println("********+++++++++@@@@@@@@@@@@@@#############"+sum);
        System.out.println("********+++++++++@@@@@@@@@@@@@@#############"+totalNum);
        //-----------------------------------------------------------------------


        // 计算总金额totalAmount   这里涉及到敏感信息,需要重新计算,不能直接使用页面的数值
        // 计算总金额 = 单价 * 数量



        // 根据页面结合map的封装
        HashMap<String, Object> hashMap = new HashMap<>();

        hashMap.put("userAddressList",userAddressList);
        hashMap.put("detailArrayList",orderDetailList);
        hashMap.put("totalNum",totalNum);
        hashMap.put("totalAmount",orderInfo.getTotalAmount());

        //页面存储流水号,防止提交和保存订单数据时,重复提交订单数据,后台有判断
        String tradeNo = this.orderService.getTradeNo(userId);
        hashMap.put("tradeNo",tradeNo);

        return Result.ok(hashMap);
    }

    // 保存/提交 订单数据
    // http://api.gmall.com/api/order/auth/submitOrder?tradeNo=null
    @PostMapping("/auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo,HttpServletRequest request){

        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setUserId(Long.parseLong(userId));

        //获取页面提交订单的订单号参数,这个参数为了防止重复提交订单而定义的.
        String tradeNo = request.getParameter("tradeNo");
        //点击提交时,已将流水号存入缓存,这里正是为了防止用户再次刷新页面重新提交,调用比较方法,与缓存中的流水号作比较
        Boolean result = this.orderService.checkTradeNo(userId, tradeNo);
        if (!result) {
            //不能提交訂單,并給信息提示
            return Result.fail().message("不能重复提交订单数据");
        }
        //删除缓存流水号
        this.orderService.delTradeNo(userId);

        Long orderId = this.orderService.saveOrderInfo(orderInfo);
        return Result.ok(orderId);
    }


}
