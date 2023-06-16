package com.atguigu.gmall.common.cache;

import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.RedisConst;
import lombok.SneakyThrows;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.bouncycastle.jcajce.provider.util.SecretKeyUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName: GmallCacheAspect
 * @author: javaermamba
 * @date: 2023-06-2023/6/16-14:32
 * @Description: 切面类
 */
@Component
@Aspect //表示使用注解方式实现aop
//AOP切面思想,切自定义注解
public class GmallCacheAspect {
    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RedisTemplate redisTemplate;

    //  如何切注解： org.springframework.transaction.annotation.Transactional 事务注解的全路径
    //  @annotation(org.springframework.transaction.annotation.Transactional)
    //  @Around("com.xyz.myapp.CommonPointcuts.businessService()") 官方注解的环绕通知: 切面是一个具体的方法
    //  joinPoint : 作用能够获取到整个通知中的所有数据：(前置通知.后置通知.环绕通知)
    //  返回值： 我们在定义方法的返回值为object,为什么不是一个具体的值呢? 因为我我们在切方法的时候,方法的返回值时不确定的类型,这里无法确定,用最大的接着

    @SneakyThrows
    @Around("@annotation(com.atguigu.gmall.common.cache.GmallCache)") //我们自定义的额环绕通知切的是一个注解
    public Object cacheAspect(ProceedingJoinPoint joinPoint){
        //保证方法正确性,声明一个Object对象
        Object object = new Object();

        //业务逻辑,切到这个注解之后,处理的事情: 添加分布式锁的业务逻辑

        /*
        1.  查询缓存中是否有数据！
                前提必须要获取到缓存中的key！
                缓存的key 组成 是由注解中定义的 prefix + 方法的参数！

                cache:param
                param: 是谁？ 这个注解在哪个方法上，我就获取哪个方法上的参数！ 得出: 要先找到注解！

        2.  获取方法上的注解：

         */
        // 获取方法上的注解
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        //获取到方法上的注解
        GmallCache gmallCache = methodSignature.getMethod().getAnnotation(GmallCache.class);
        //获取直接的前缀
        String prefix = gmallCache.prefix();
        // 获取方法参数
        Object[] args = joinPoint.getArgs();
        // 组成缓存key(将数组变成集合形式,toString拼上),不然直接拼接的是数组了
        // prefix[24]
        String key = prefix + Arrays.asList(args).toString();

        try {
            //得到缓存的key后,就可以获取缓存中的数据了
            //定义方法,专门根据key值,到缓存中获取数据
            // 可返回值可能代表：skuInfo 也可能代表 List<SpuSaleAttr>
            object = this.getRedisData(key,methodSignature);
            //对返回值object进行判断
            if (object==null){
                // 为空为true 代表redis中没有缓存,这是就需要去数据库中获取数据了,同时也意味着需要经过redisson的锁,只放行一个请求
                // redisson上锁(锁的key不能重)
                String lockKey = prefix + ":lock";
                //调用redisson分布式锁
                RLock lock = this.redissonClient.getLock(lockKey);

                // 尝试上锁
                boolean result = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);

                //判断是否拿到锁
                if (result){
                    try {
                        //true 拿到锁查询数据库
                        // 如何查询数据库? 使用joinPoint对象可以获取到方法执行时,执行后等很多数据
                        // 调用proceed,相当于执行了注解下的方法体中的方法了
                        // joinPoint.getArgs()方法:可以获取到注解下方法体中方法的对应参数获取到
                        // 所以,可以执行注解下的方法体了
                        // 此外,这里用object接收,1.基类 2.object执行到此处还为空
                        object = joinPoint.proceed(joinPoint.getArgs());
                        // 执行完查询数据库操作,判断查询结果
                        if (object == null){
                            //数据库中没有对应数据,创建一个空对象放入到缓存中
                            Object o = new Object();
                            // 将数据放入缓存中
                            // 将新建空o对象转成字符串放入缓存中,因为o代表任意类型的数据对象,所以将对象统一转换为对应字符即可
                            this.redisTemplate.opsForValue().set(key, JSON.toJSONString(o),RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                            return o;
                        }else {
                            //数据库中有对应数据,直接返回 ( 不管你返回的是skuInfo ,还是List<SpuSaleAttr> 都统一将其转化为字符串存储到缓存。)
                            this.redisTemplate.opsForValue().set(key, JSON.toJSONString(object), RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                            return object;
                        }
                    }finally {
                        //解锁
                        lock.unlock();
                    }
                }else{
                    //false 没有拿到锁,自旋等待
                    Thread.sleep(300);
                    // 自旋,重新执行方法
                    return cacheAspect(joinPoint);
                }
            }else{
                //不为空.redis中有缓存,直接返回即可
                return object;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        // 如果redis宕机了.使用数据库兜底
        return joinPoint.proceed(joinPoint.getArgs());
    }

    /**
     *  定义方法: 根据key从缓存中获取数据
     * @param key
     * @return
     */
    private Object getRedisData(String key,MethodSignature methodSignature) {
        //缓存中存的数据是什么类型? 字符串类型
        String strJson = (String) this.redisTemplate.opsForValue().get(key);
        // 获取数据. 目的: 渲染到页面上
        // 返回就要是具体的数据类型给页面展示了
        if (!StringUtils.isEmpty(strJson)){
            //缓存中获取到数据时:
            // strJson 如果存储的是skuInfo 类型 ，则将其转换 skuInfo 对象！
            // strJson 如果存储的是List<SpuSaleAttr> 类型 ，则将其转换为 List<SpuSaleAttr>
            // 如何知道具体的返回对象类型呢? 使用MethodSignature对象下的一个方法,可以获取到返回值的类型,我们将这个对象当做参数传到本方法中
            // 返回具体的数据类型。
            return JSON.parseObject(strJson,methodSignature.getReturnType());
        }
        //默认返回空
        return null;
    }
}