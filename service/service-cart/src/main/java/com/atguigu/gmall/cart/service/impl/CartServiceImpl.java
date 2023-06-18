package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * @ClassName: CartServiceImpl
 * @author: javaermamba
 * @date: 2023-06-2023/6/18-12:42
 * @Description:
 */
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Qualifier("com.atguigu.gmall.product.client.ProductFeignClient")
    @Autowired
    private ProductFeignClient productFeignClient;

    @Override
    public void addToCart(Long skuId, String userId, Integer skuMun) {

        /*
            1. 判断当前添加商品是否存在在购物车

            2. 每次添加购物车数据是默认是选中状态

            3.保证放入的商品的数据价格是最新的

            4. 购物车的排序规则   加入时的时间排序:苏宁
         */

        // 购物车key
        String cartKey = getCartKey(userId);

        // 看缓存中是否存在这个购物项
        CartInfo cartInfoExist = (CartInfo) this.redisTemplate.opsForHash().get(cartKey, skuId.toString());

        if (cartInfoExist!=null) {
            //当前项存在
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuMun);
            //如果选中状态1 不用改 是0 才改
            if (cartInfoExist.getIsChecked().intValue()==0){
                cartInfoExist.setIsChecked(1);
            }

            //保证数据价格是最新的
            cartInfoExist.setSkuPrice(productFeignClient.getskuPrice(skuId));

            // 加入购物项的时间进行排序  重新赋值修改时间
            cartInfoExist.setUpdateTime(new Date());

//            //再次放入缓存
//            this.redisTemplate.opsForHash().put(cartKey,skuId.toString(),cartInfoExist);

        }else {

            /*
                购物项不存在缓存中 直接添加即可
             */
            cartInfoExist = new CartInfo();
            SkuInfo skuInfo = this.productFeignClient.getSkuInfo(skuId);

            cartInfoExist.setSkuId(skuInfo.getId());
            cartInfoExist.setSkuName(skuInfo.getSkuName());
            cartInfoExist.setSkuNum(skuMun);
            //第一次加入缓存,加入购物车的价格要与实时价格一致
            cartInfoExist.setSkuPrice(this.productFeignClient.getskuPrice(skuId));
            cartInfoExist.setCartPrice(this.productFeignClient.getskuPrice(skuId));
            cartInfoExist.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfoExist.setUserId(userId);
            cartInfoExist.setUpdateTime(new Date());
            cartInfoExist.setCreateTime(new Date());
        }

        //放入缓存
        this.redisTemplate.opsForHash().put(cartKey,skuId.toString(),cartInfoExist);

    }

    private String getCartKey(String userId) {
        String cartKey = RedisConst.USER_KEY_PREFIX+ userId +RedisConst.USER_CART_KEY_SUFFIX;
        return cartKey;
    }
}
