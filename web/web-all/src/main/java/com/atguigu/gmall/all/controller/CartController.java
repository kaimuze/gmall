package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * @ClassName: CartController
 * @author: javaermamba
 * @date: 2023-06-2023/6/18-17:12
 * @Description: 购物车
 */
@Controller
public class CartController {

    @Qualifier("com.atguigu.gmall.product.client.ProductFeignClient")
    @Autowired
    private ProductFeignClient productFeignClient;

    // 添加购物车按钮  item.html 跳转到 addCart.html页面
    // http://cart.gmall.com/addCart.html?skuId=24&skuNum=1&sourceType=query
    @GetMapping("addCart.html")
    public String cart(HttpServletRequest request){

        // 需要在后天存储skuInfo skuNum
        // 商品详情页面加入购物车是异步操作
        String skuId = request.getParameter("skuId");
        String skuNum = request.getParameter("skuNum");
        SkuInfo skuInfo = this.productFeignClient.getSkuInfo(Long.parseLong(skuId));

        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("skuNum",skuNum);

        //返回视图
        return "cart/addCart.html";
    }

    //查看购物车列表
    @GetMapping("cart.html")
    public String cartList(){
        // 页面异步请求获取数据

        return "cart/index";
    }

}
