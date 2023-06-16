package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @ClassName: IndexController
 * @author: javaermamba
 * @date: 2023-06-2023/6/16-18:58
 * @Description: 访问首页分类数据控制器
 */
@Controller
public class IndexController {

    @Qualifier("com.atguigu.gmall.product.client.ProductFeignClient")
    @Autowired
    private ProductFeignClient productFeignClient;

    // www.gmall.com 或 www.gmall.com/index.html
    @GetMapping({"/","index.html"})
    public String index(Model model){

        Result result = productFeignClient.getBaseCategoryList();
        model.addAttribute("list", result.getData());

        return "index/index";
    }

}
