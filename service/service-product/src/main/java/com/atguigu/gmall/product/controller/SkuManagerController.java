package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.service.SkuManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ClassName: SkuManagerController
 * @author: javaermamba
 * @date: 2023-06-2023/6/15-11:09
 * @Description: sku相关控制器
 */
@RestController
@RequestMapping("/admin/product/")
public class SkuManagerController {

    @Autowired
    private SkuManagerService skuManagerService;

    @PostMapping("saveSkuInfo")
    public Result saveSkuInfo(@RequestBody SkuInfo skuInfo){
        this.skuManagerService.saveSkuInfo(skuInfo);
        return Result.ok();
    }


}
