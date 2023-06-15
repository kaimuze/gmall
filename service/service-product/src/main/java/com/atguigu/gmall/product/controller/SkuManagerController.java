package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.service.SkuManagerService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    // 商品属性的sku管理 http://localhost/admin/product/list/1/10?category3Id=61
    @GetMapping("list/{page}/{limit}")
    public Result listSku(@PathVariable Long page,
                          @PathVariable Long limit,
                          @RequestParam Long category3Id){
        Page<SkuInfo> pageModel = new Page<>(page,limit);
        IPage<SkuInfo> iPage = this.skuManagerService.getListSku(pageModel,category3Id);
        return Result.ok(iPage);
    }

    // 上架下架操作
    // http://localhost/admin/product/cancelSale/20
    // http://localhost/admin/product/onSale/21
    @GetMapping("onSale/{skuId}")
    public Result onSale(@PathVariable Long skuId){
        this.skuManagerService.onSale(skuId);
        return Result.ok();
    }

    @GetMapping("cancelSale/{skuId}")
    public Result cancelSale(@PathVariable Long skuId){
        this.skuManagerService.cancelSale(skuId);
        return Result.ok();
    }

}
