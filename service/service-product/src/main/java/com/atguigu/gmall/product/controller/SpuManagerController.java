package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.product.service.ManagerService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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


}
