package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.atguigu.gmall.product.service.BaseCategoryTrademarkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @ClassName: BaseCategoryTrademarkController
 * @author: javaermamba
 * @date: 2023-06-2023/6/14-11:24
 * @Description:
 */
@RestController
@RequestMapping("/admin/product/baseCategoryTrademark")
public class BaseCategoryTrademarkController {

    @Autowired
    private BaseCategoryTrademarkService baseCategoryTrademarkService;

    /**
     * 根据三级分类id查询品牌分类数据
     * @param category3Id
     * @return
     */
    // http://localhost/admin/product/baseCategoryTrademark/findTrademarkList/61
    @GetMapping("findTrademarkList/{category3Id}")
    public Result findTrademarkList(@PathVariable Long category3Id){
        List<BaseTrademark> baseCategoryTrademarkList = this.baseCategoryTrademarkService.getTrademarkList(category3Id);
        return Result.ok(baseCategoryTrademarkList);
    }

    /**
     * 查询当前三级分类id下的品牌分类数据
     * @param category3Id
     * @return
     */
    //http://localhost/admin/product/baseCategoryTrademark/findCurrentTrademarkList/61
    @GetMapping("findCurrentTrademarkList/{category3Id}")
    public Result findCurrentTrademarkList(@PathVariable Long category3Id){
        List<BaseTrademark> baseTrademarkList = this.baseCategoryTrademarkService.getCurrentCategoryTrademarkList(category3Id);
        return Result.ok(baseTrademarkList);
    }

    /**
     * 保存品牌分类数据 保存分类与品牌的关系
     * @param categoryTrademarkVo
     * @return
     */
    // http://localhost/admin/product/baseCategoryTrademark/save
    @PostMapping("save")
    public Result saveBaseTrademark(@RequestBody CategoryTrademarkVo categoryTrademarkVo){
        this.baseCategoryTrademarkService.save(categoryTrademarkVo);
        return Result.ok();
    }

    /**
     * 删除分类与品牌关系列表数据  http://localhost/admin/product/baseCategoryTrademark/remove/61/9
     */
    @DeleteMapping("/remove/{category3Id}/{trademarkId}")
    public Result removeTrademark(@PathVariable Long category3Id,@PathVariable Long trademarkId){

        this.baseCategoryTrademarkService.removeTrademark(category3Id,trademarkId);
        return Result.ok();

    }

}
