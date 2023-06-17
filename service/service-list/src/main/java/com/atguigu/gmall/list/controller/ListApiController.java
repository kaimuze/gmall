package com.atguigu.gmall.list.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.Goods;
import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName: ListApiController
 * @author: javaermamba
 * @date: 2023-06-2023/6/16-20:40
 * @Description:
 */
@RestController
@RequestMapping("/api/list")
public class ListApiController {

    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Autowired
    private SearchService searchService;

    @GetMapping("inner/createIndex")
    public Result createIndex(){

        //创建索引库
        restTemplate.createIndex(Goods.class);
        //创建映射
        restTemplate.putMapping(Goods.class);

        return Result.ok();
    }

    /**
     * es商品上下架  访问此控制器创建索引库 7版本后无需访问,自动创建,前提是有接口继承 public interface GoodsRepository extends ElasticsearchRepository<Goods,Long>
     * @param skuId
     * @return
     */
    @GetMapping("inner/upperGoods/{skuId}")
    public Result upperGoods(@PathVariable Long skuId){
        this.searchService.upperGoods(skuId);
        return Result.ok();
    }

    /**
     * es商品上下架
     * @param skuId
     * @return
     */
    @GetMapping("inner/lowerGoods/{skuId}")
    public Result lowerGoods(@PathVariable Long skuId){
        this.searchService.lowerGoods(skuId);
        return Result.ok();
    }

    /**
     * 触发热度排名
     * @param skuId
     * @return
     */
    @GetMapping("inner/incrHotScore/{skuId}")
    public Result incrHotScore(@PathVariable Long skuId){
        this.searchService.incrHotScore(skuId);
        return Result.ok();
    }

    @PostMapping
    public Result searchList(@RequestBody SearchParam searchParam){

        SearchResponseVo responseVo = null;
        try {
            responseVo = this.searchService.search(searchParam);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Result.ok(responseVo);

    }

}
