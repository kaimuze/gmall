package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.atguigu.gmall.product.service.ManagerService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import sun.net.idn.Punycode;

/**
 * @ClassName: BaseTradeMarkController
 * @author: javaermamba
 * @date: 2023-06-2023/6/6-21:11
 * @Description: 品牌管理
 */
@RestController
@RequestMapping("/admin/product/baseTrademark")
public class BaseTradeMarkController {

    @Autowired
    private ManagerService managerService;

    @Autowired
    private BaseTrademarkService baseTrademarkService;

    /**
     * 获取品牌列表数据(分页)
     * @param page
     * @param limit
     * @return
     */
    // //admin/product/baseTrademark/{page}/{limit}
    @GetMapping("/{page}/{limit}")
    public Result getBaseTradeMarkList(@PathVariable Long page, @PathVariable Long limit){

        Page<BaseTrademark> pageModel = new Page<>(page,limit);
        IPage<BaseTrademark> trademarkList = this.managerService.getTrademarkList(pageModel);

        return Result.ok(trademarkList);
    }

    /**
     * 删除品牌列表数据
     * @param id
     * @return
     */
    // localhost/admin/product/baseTrademark/remove/6
    @DeleteMapping("/remove/{id}")
    public Result removeBaseTradeMarkById(@PathVariable Long id){
        this.baseTrademarkService.removeTrademarkById(id);

        //方式二: 实现IService接口
//        this.baseTrademarkService.removeById(id);
        return Result.ok();
    }

    // http://localhost/admin/product/baseTrademark/save     保存
    // http://localhost/admin/product/baseTrademark/get/6    修改

    @PostMapping("save")
    public Result saveTrademark(@RequestBody BaseTrademark baseTrademark){
        this.baseTrademarkService.saveTrademark(baseTrademark);
        //方式二: 实现IService接口
//        this.baseTrademarkService.save(baseTrademark);
        return Result.ok();
    }

    /**
     * 根据品牌id获取品牌对象 http://localhost/admin/product/baseTrademark/get/6
     * @param id
     * @return
     */
    @GetMapping("get/{id}")
    public Result getTrademarkById(@PathVariable Long id){
        BaseTrademark baseTrademark = this.baseTrademarkService.getById(id);
        return Result.ok(baseTrademark);
    }

    /**
     * 修改
     * @param baseTrademark
     * @return
     */
    //http://localhost/admin/product/baseTrademark/update
    @PutMapping("update")
    public Result updateTrademark(@RequestBody BaseTrademark baseTrademark){
        this.baseTrademarkService.updateById(baseTrademark);
        return Result.ok();
    }

}
