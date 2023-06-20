package com.atguigu.gmall.payment.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName: AlipayController
 * @Project: gmall-parent
 * @Package: com.atguigu.gmall.payment.controller
 * @Author: carry.kaimuze
 * @Date: 2023-2023/6/20-19:19
 * @Description:
 */
@Controller
@RequestMapping("/api/payment/alipay")
public class AlipayController {

    @Autowired
    private AlipayService alipayService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${app_id}")
    private String app_id;

    // http://api.gmall.com/api/payment/alipay/submit/49
    @GetMapping("/submit/{orderId}")
    @ResponseBody
    public String aliPay(@PathVariable Long orderId){

        String form = null;
        try {
            form = this.alipayService.createAlipay(orderId);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        //将form输出到页面
        return form;
    }


    //同步回调地址: http://api.gmall.com/api/payment/alipay/callback/return
    @GetMapping("callback/return")
    public String callbackReturn(){
        //重定向到web-all控制器
        // http://payment.gmall.com/pay/success.html
        return "redirect:" + AlipayConfig.return_order_url;

    }

    //发送异步回调 支付完成后,由支付宝主动发送
    // 异步回调  http://rjsh38.natappfree.cc  需要使用内网穿透映射进来
    // notify_payment_url: http://rjsh38.natappfree.cc/api/payment/alipay/callback/notify
    //  https://商家网站通知地址?voucher_detail_list=[{"amount":"0.20","merchantContribute":"0.00","name":"5折券","otherContribute":"0.20","type":"ALIPAY_DISCOUNT_VOUCHER","voucherId":"2016101200073002586200003BQ4"}]&fund_bill_list=[{"amount":"0.80","fundChannel":"ALIPAYACCOUNT"},{"amount":"0.20","fundChannel":"MDISCOUNT"}]&subject=PC网站支付交易&trade_no=2016101221001004580200203978&gmt_create=2016-10-12 21:36:12&notify_type=trade_status_sync&total_amount=1.00&out_trade_no=mobile_rdm862016-10-12213600&invoice_amount=0.80&seller_id=2088201909970555&notify_time=2016-10-12 21:41:23&trade_status=TRADE_SUCCESS&gmt_payment=2016-10-12 21:37:19&receipt_amount=0.80&passback_params=passback_params123&buyer_id=2088102114562585&app_id=2016092101248425&notify_id=7676a2e1e4e737cff30015c4b7b55e3kh6& sign_type=RSA2&buyer_pay_amount=0.80&sign=***&point_amount=0.00
    // @RequestParam Map<String,String> paramMap :将url 路径上的参数变为 Map集合
    //将异步通知中收到的所有参数都存放到map中
    @PostMapping("callback/notify")
    public String callBackNotify(@RequestParam Map<String,String> paramMap){

        // 校验
        boolean signVerified = false; //调用SDK验证签名
        try {
            signVerified = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        // 获取参数中的out_trade_no
        String outTradeNo = paramMap.get("out_trade_no");
        //获取参数中的total_amount
        String totalAmount = paramMap.get("total_amount");
        // 获取参数中的app_id
        String appId = paramMap.get("app_id");
        // 获取参数中的支付状态
        String tradeStatus = paramMap.get("trade_status");
        // 获取notify_id: 服务器异步通知参数
        String notifyId = paramMap.get("notify_id");


        if(signVerified){
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure


            // 第五步: 需要再次验证三个参数 out_trade_no total_amount app_id
            // 验证out_trade_no的一致性  如果可以拿到paymentInfo,就说明订单是正确的
            PaymentInfo paymentInfoQuery = this.paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());
//            if (paymentInfoQuery==null || paymentInfoQuery.getTotalAmount().compareTo(new BigDecimal(totalAmount))!=0){
            // 上线是这样写的,但是我们的支付是测试阶段,金额定死为0.01了
            if (paymentInfoQuery==null || new BigDecimal("0.01").compareTo(new BigDecimal(totalAmount))!=0
                    || !appId.equals(app_id)){
                // 如果不空则证明,out_trade_no不一致的 || 前后的总金额也不一样 || appId前后不一致
                // 返回失败
                return "failure";
            }

            // 因为当程序没有出现success七个字符的话,说明支付是失败的,只要失败服务器异步同志参数notify_id就会一致存在,所以,我们需要在失败的过程中将notify_id存在缓存中,等待成功后,将其清除
            // setnx : 判断当前key是否存在 ,存在true 不存在 false
            // setex : 失效时间 24小时22分钟
            // set key value ex/px timeout nx/xx
            // 第一次异步回调有数据,result=true 放入缓存
            Boolean result = this.redisTemplate.opsForValue().setIfAbsent(notifyId, notifyId, 1462 * 60, TimeUnit.SECONDS);
            if (!result){ // 这里只有true能进来
                // 说明缓存中有数据. 这时的情况是在二次验证时可能出现网络抖动等异常卡住了
                return "failure";
            }

            // 二次验证: 获取支付状态
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)){
                // 支付成功. 需要修改交易记录状态
                this.paymentService.paySuccess(outTradeNo,PaymentType.ALIPAY.name(),paramMap);
                return "success";
            }
        }else{
            // TODO 验签失败则记录异常日志，并在response中返回failure.

            return "failure";
        }
        // success failure
        return "";
    }


}
