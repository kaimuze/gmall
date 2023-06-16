package com.atguigu.gmall.list.repository;

import com.atguigu.gmall.model.list.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @author 恺牧泽
 */

public interface GoodsRepository extends ElasticsearchRepository<Goods,Long> {



}
