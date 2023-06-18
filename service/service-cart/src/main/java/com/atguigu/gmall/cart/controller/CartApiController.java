package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @ClassName: CartApiController
 * @author: javaermamba
 * @date: 2023-06-2023/6/18-13:08
 * @Description:
 */
@RestController
@RequestMapping("/api/cart")
public class CartApiController {

    @Autowired
    private CartService cartService;

    // /api/cart/addToCart/{skuId}/{skuNum}
    @GetMapping("/addToCart/{skuId}/{skuNum}")
    public Result addToCart(@PathVariable Long skuId, @PathVariable Integer skuNum, HttpServletRequest request) {

        String userId = AuthContextHolder.getUserId(request);

        if (StringUtils.isEmpty(userId)) {
            userId = AuthContextHolder.getUserTempId(request);
        }

        this.cartService.addToCart(skuId, userId, skuNum);

        return Result.ok();

    }

    // 查询用户购物车信息
    @GetMapping("cartList")
    public Result cartList(HttpServletRequest request){

        String userId = AuthContextHolder.getUserId(request);
        String userTempId = AuthContextHolder.getUserTempId(request);

        List<CartInfo> cartInfoList = this.cartService.getCartList(userId,userTempId);
        return Result.ok(cartInfoList);
    }


    //选中状态变更
    @GetMapping("checkCart/{skuId}/{isChecked}")
    public Result checkCart(@PathVariable Long skuId,
                            @PathVariable Integer isChecked,
                            HttpServletRequest request){

        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)){
            userId = AuthContextHolder.getUserTempId(request);
        }

        this.cartService.checkCart(skuId,isChecked,userId);

        return Result.ok();
    }

    //删除购物车购物项
    @DeleteMapping("deleteCart/{skuId}")
    public Result deleteCart(@PathVariable Long skuId,HttpServletRequest request){

        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)){
            userId  = AuthContextHolder.getUserTempId(request);
        }

        this.cartService.deleteCart(skuId,userId);

        return Result.ok();
    }


}
