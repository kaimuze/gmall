package com.atguigu.gmall.list.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.list.Goods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("inner/createIndex")
    public Result createIndex(){

        //创建索引库
        restTemplate.createIndex(Goods.class);
        //创建映射
        restTemplate.putMapping(Goods.class);

        return Result.ok();
    }

}
