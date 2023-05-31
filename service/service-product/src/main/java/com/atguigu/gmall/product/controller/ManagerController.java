package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @ClassName: ManagerController
 * @author: javaermamba
 * @date: 2023-05-2023/5/25-14:49
 * @Description: 后台管理系统-平台属性列表控制器
 */
@RestController
@RequestMapping("/admin/product")
public class ManagerController {

    @Autowired
    private ManagerService managerService;

    @GetMapping("/getCategory1")
    public Result getCategory1(){

        List<BaseCategory1> baseCategory1List = managerService.getCategory1();

        return Result.ok(baseCategory1List);
    }

    @GetMapping("/getCategory2/{category1Id}")
    public Result getCategory2(@PathVariable Long category1Id){

        List<BaseCategory2> baseCategory2List = this.managerService.getCategory2(category1Id);
        return Result.ok(baseCategory2List);
    }

    @GetMapping("/getCategory3/{category2Id}")
    public Result getCategory3(@PathVariable Long category2Id){
        List<BaseCategory3> baseCategory3List = this.managerService.getCategory3(category2Id);
        return Result.ok(baseCategory3List);
    }


    // /admin/product/attrInfoList/{category1Id}/{category2Id}/{category3Id}
    // 根据分类id获取平台属性数据
    @GetMapping("attrInfoList/{category1Id}/{category2Id}/{category3Id}")
    public Result getAttrInfoListByCategoryId(@PathVariable Long category1Id,
                                              @PathVariable Long category2Id,
                                              @PathVariable Long category3Id){

        List<BaseAttrInfo> baseAttrInfoList = this.managerService.getAttrInfoListByCategoryId(category1Id, category2Id, category3Id);
        return Result.ok(baseAttrInfoList);
    }

    //添加平台属性 Json-->POJO
    @PostMapping("saveAttrInfo")
    public Result saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){

        this.managerService.saveAttrInfo(baseAttrInfo);
        return Result.ok();
    }

    /**
     * 平台属性修改操作: 第一步回显操作
     * @param attrId
     * @return
     */
    @GetMapping("getAttrValueList/{attrId}")
    public Result getAttrValueList(@PathVariable Long attrId){

        //先根据这个平台属性id获取到平台属性对象
        BaseAttrInfo baseAttrInfo = this.managerService.getBaseAttrInfo(attrId);
        //List<BaseAttrValue> attrValueList = this.managerService.getAttrValueList(attrId);
        //有属性的情况下,再获取相对应的平台属性值,这样做算是细化了查询平台属性时,属性值在那一瞬间出现更改导致后续的查找不存在的情况
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        return Result.ok(attrValueList);
    }
}
