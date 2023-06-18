package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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

    @Override
    public List<CartInfo> getCartList(String userId, String userTempId) {

        //声明为登录购物车集合数据对象
        List<CartInfo> cartInfoNoLoginList = new ArrayList<>();

        // 判断当前用户id 是否是登录状态
        if (!StringUtils.isEmpty(userTempId)){
            //登录了
            String cartKey = this.getCartKey(userId);
            //获取登录的购物车数据
            cartInfoNoLoginList = this.redisTemplate.opsForHash().values(cartKey);
        }

        // 查询之后 排序购物项  按照修改时间进行排序 未登录
        if (!CollectionUtils.isEmpty(cartInfoNoLoginList)){
            cartInfoNoLoginList.sort( (o1, o2) -> {
                return DateUtil.truncatedCompareTo(o2.getUpdateTime(), o1.getUpdateTime(), Calendar.SECOND);
            });

            //返回未登录购物车集合数据
            return cartInfoNoLoginList;
        }

        //++++++++++++++++++++++上: 未登录  下:登录++++++++++++++++++++++++++++++++++++++
        if (!StringUtils.isEmpty(userId)){
            String cartKey = this.getCartKey(userId);
            // BoundHashOperations<H, HK, HV>  H:缓存key的数据类型 HK:field数据类型 HV:value的数据类型
            BoundHashOperations<String,String,CartInfo> boundHashOperations = this.redisTemplate.boundHashOps(cartKey);
//            boundHashOperations.get(); 获取value数据方法

            // 合并情况:  只有未登录购物车中有数据的情况涉及到合并
            if (!CollectionUtils.isEmpty(cartInfoNoLoginList)){
                cartInfoNoLoginList.stream().forEach(cartInfoNoLogin -> {
                    //判断   登录购物车中是否包含未登录购物车相同skuid的购物项
                    if (boundHashOperations.hasKey(cartInfoNoLogin.getSkuId().toString())){
                        // 包含   则累加
                        CartInfo cartInfoLogin = boundHashOperations.get(cartInfoNoLogin.getSkuId().toString());
                        // 未登录向登录合并数据
                        // 数量合并
                        cartInfoLogin.setSkuNum(cartInfoLogin.getSkuNum()+cartInfoNoLogin.getSkuNum());
                        cartInfoLogin.setUpdateTime(new Date());

                        // 合并细节 选中状态的合并
                        if (cartInfoNoLogin.getIsChecked().intValue() == 1 && cartInfoLogin.getIsChecked().intValue() == 0){
                            cartInfoLogin.setIsChecked(1);
                        }

                        //更新好的数据写入缓存
                        boundHashOperations.put(cartInfoLogin.getSkuId().toString(),cartInfoLogin);

                    }else {
                        // 未包含
                        // 覆盖未登录的用户id 因为未登录用户id是临时用户id
                        cartInfoNoLogin.setUserId(userId);
                        cartInfoNoLogin.setCreateTime(new Date());
                        cartInfoNoLogin.setUpdateTime(new Date());
                        // 直接写入缓存
                        boundHashOperations.put(cartInfoNoLogin.getSkuId().toString(),cartInfoNoLogin);
                    }

                });
            }

            // 删除未登录购物车集合数据
            this.redisTemplate.delete(this.getCartKey(userTempId));

            // 获取到合并后的数据
            List<CartInfo> cartInfoLoginList = boundHashOperations.values();
            // 排序
            if (!CollectionUtils.isEmpty(cartInfoLoginList)){
                cartInfoLoginList.sort((o1, o2) -> {
                    return DateUtil.truncatedCompareTo(o2.getUpdateTime(),o1.getUpdateTime(),Calendar.SECOND);
                });
            }

            // 返回登录之后的数据
            return cartInfoLoginList;
        }

        //返回购物车集合
        return new ArrayList<>();
    }

    @Override
    public void checkCart(Long skuId, Integer isChecked, String userId) {
        // 获取缓存key
        String cartKey = this.getCartKey(userId);
        CartInfo cartInfo = (CartInfo) this.redisTemplate.boundHashOps(cartKey).get(skuId.toString());
        //等价于
//        this.redisTemplate.opsForHash().get(cartKey,skuId.toString());
        if (cartInfo != null) {
            cartInfo.setIsChecked(isChecked);
            this.redisTemplate.boundHashOps(cartKey).put(skuId.toString(),cartInfo);
        }
    }

    @Override
    public void deleteCart(Long skuId, String userId) {
        String cartKey = this.getCartKey(userId);
        // redis判断是否存在
        Boolean result = this.redisTemplate.boundHashOps(cartKey).hasKey(skuId.toString());
        if (result){
            this.redisTemplate.boundHashOps(cartKey).delete(skuId.toString());
        }
    }

    private String getCartKey(String userId) {
        String cartKey = RedisConst.USER_KEY_PREFIX+ userId +RedisConst.USER_CART_KEY_SUFFIX;
        return cartKey;
    }
}
