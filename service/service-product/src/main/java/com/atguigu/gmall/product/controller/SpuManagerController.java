package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseSaleAttr;
import com.atguigu.gmall.model.product.SpuImage;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.model.product.SpuPoster;
import com.atguigu.gmall.product.service.ManagerService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @ClassName: SpuManagerController
 * @author: javaermamba
 * @date: 2023-06-2023/6/6-19:56
 * @Description: spu数据控制器
 */
@RestController
@RequestMapping("/admin/product")
public class SpuManagerController {

    @Autowired
    private ManagerService managerService;

    // /admin/product/{page}/{limit}
    // 请求网址:
    //http://localhost/admin/product/1/10?category3Id=13
    // category3Id=13 有多种方法取到,1.@RequestParam 2.实体类 SpuInfo(SpringMvc支持对象传值) 3.HttpServletRequest request.getParameter("属性名")
    @GetMapping("{page}/{limit}")
    public Result getSpuInfoList(@PathVariable Long page,
                                 @PathVariable Long limit,
                                 SpuInfo spuInfo){

        // select * from spu_info where category3_id = 61 limit 1,0;
        Page<SpuInfo> pageModel = new Page<>(page,limit);

        IPage<SpuInfo> iPage = this.managerService.getSpuInfoList(pageModel,spuInfo);
        return Result.ok(iPage);

    }

    // http://localhost/admin/product/baseSaleAttrList 获取spu所有的销售属性
    @GetMapping("baseSaleAttrList")
    public Result getSaleAttrList(){

        List<BaseSaleAttr> baseSaleAttrList = this.managerService.getSaleAttrList();
        return Result.ok(baseSaleAttrList);
    }

    // http://localhost/admin/product/saveSpuInfo
    // {id: null, spuName: null, description: null, category3Id: 61, spuImageList: [], spuSaleAttrList: [],…}
    @PostMapping("saveSpuInfo")
    public Result saveSpuInfo(@RequestBody SpuInfo spuInfo){
        this.managerService.saveSpuInfo(spuInfo);

        return Result.ok();
    }


}
