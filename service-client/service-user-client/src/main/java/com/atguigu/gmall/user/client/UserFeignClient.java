package com.atguigu.gmall.user.client;

import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.user.client.impl.UserDegradeFeignClientImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * @author 恺牧泽
 */
@FeignClient(value = "service-user",fallback = UserDegradeFeignClientImpl.class)
public interface UserFeignClient {


    @GetMapping("/api/user/inner/inner/findUserAddressListByUserId/{userId}")
    List<UserAddress> findUserAddressListByUserId(@PathVariable Long userId);

}
