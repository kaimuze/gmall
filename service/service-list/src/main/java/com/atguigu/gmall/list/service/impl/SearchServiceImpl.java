package com.atguigu.gmall.list.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.list.repository.GoodsRepository;
import com.atguigu.gmall.model.list.*;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName: SearchServiceImpl
 * @author: javaermamba
 * @date: 2023-06-2023/6/16-21:31
 * @Description:
 */
@Service
public class SearchServiceImpl implements SearchService {

    //调用service-product这个微服务查询数据库
    @Qualifier("com.atguigu.gmall.product.client.ProductFeignClient")
    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RestHighLevelClient restHighLevelClient;


    /**
     * 上架的本质: 首先给Goods赋值,然后将Goods保存到es索引库中
     *
     * @param skuId
     */
    @Override
    public void upperGoods(Long skuId) {
        Goods goods = new Goods();

        SkuInfo skuInfo = this.productFeignClient.getSkuInfo(skuId);
        goods.setId(skuId);
        goods.setTitle(skuInfo.getSkuName());
        goods.setDefaultImg(skuInfo.getSkuDefaultImg());
        goods.setPrice(productFeignClient.getskuPrice(skuId).doubleValue());
        goods.setCreateTime(new Date());

        //赋值品牌数据
        BaseTrademark trademark = productFeignClient.getTrademark(skuInfo.getTmId());
        goods.setTmId(trademark.getId());
        goods.setTmName(trademark.getTmName());
        goods.setTmLogoUrl(trademark.getLogoUrl());

        //赋值分类属性
        BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
        goods.setCategory1Id(categoryView.getCategory1Id());
        goods.setCategory2Id(categoryView.getCategory2Id());
        goods.setCategory3Id(categoryView.getCategory3Id());

        goods.setCategory1Name(categoryView.getCategory1Name());
        goods.setCategory2Name(categoryView.getCategory2Name());
        goods.setCategory3Name(categoryView.getCategory3Name());

        //商品热度初始化值 都是0

        // 赋值平台属性和属性值 private List<SearchAttr> attrs;
//        ArrayList<SearchAttr> attrs = new ArrayList<>();
//        List<BaseAttrInfo> attrList = this.productFeignClient.getAttrList(skuId);
//        attrList.forEach(attr ->{
//            SearchAttr searchAttr = new SearchAttr();
//            searchAttr.setAttrId(attr.getId());
//            searchAttr.setAttrName(attr.getAttrName());
//            searchAttr.setAttrValue(attr.getAttrValueList().get(0).getValueName());
//            attrs.add(searchAttr);
//        });
        List<BaseAttrInfo> attrsList = this.productFeignClient.getAttrList(skuId);
        List<SearchAttr> attrs = attrsList.stream().map(baseAttrInfo -> {
            SearchAttr searchAttr = new SearchAttr();
            searchAttr.setAttrId(baseAttrInfo.getId());
            searchAttr.setAttrName(baseAttrInfo.getAttrName());
            searchAttr.setAttrValue(baseAttrInfo.getAttrValueList().get(0).getValueName());
            return searchAttr;
        }).collect(Collectors.toList());
        goods.setAttrs(attrs);

        //将goods保存到es中
        this.goodsRepository.save(goods);
    }

    @Override
    public void lowerGoods(Long skuId) {
        this.goodsRepository.deleteById(skuId);
    }

    @Override
    public void incrHotScore(Long skuId) {
        //本质是更新es中hotScore字段的数据 每访问一次,更新一次.这样会伤害性能,每次更新都有io
        //借助redis 记录当前这个商品被访问的次数,如果访问次数达到一定数量,再去更新
        String hotKey = "hotScore";
        Double count = this.redisTemplate.opsForZSet().incrementScore(hotKey, "skuId:" + skuId, 1);
        if (count % 10 == 0) {
            //更新es
            Optional<Goods> optional = this.goodsRepository.findById(skuId);
            Goods goods = optional.get();
            goods.setHotScore(count.longValue());
            //将更新后的数据放入es
            this.goodsRepository.save(goods);
        }
    }

    @Override
    public SearchResponseVo search(SearchParam searchParam) throws Exception {
        //声明对象
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        /*
            private List<SearchResponseTmVo> trademarkList;
            private List<SearchResponseAttrVo> attrsList = new ArrayList<>();
            private List<Goods> goodsList = new ArrayList<>();
            private Long total;//总记录数
            private Integer pageSize;//每页显示的内容
            private Integer pageNo;//当前页面
            private Long totalPages;
         */
        // 准备es的dsl语句
        SearchRequest searchRequest = this.buildQueryDsl(searchParam);
        // 执行dsl语句
        SearchResponse searchResponse = this.restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        //获取返回结果集 searchResponse --> searchResponseVo
        searchResponseVo = this.parseSearchResult(searchResponse);


        searchResponseVo.setPageNo(searchParam.getPageNo());
        searchResponseVo.setPageSize(searchParam.getPageSize());
        // 总页数 = 总条数/每页显示大小
//        Long totalPages =
//                searchResponseVo.getTotal() % searchParam.getPageSize() == 0 ?
//                        searchResponseVo.getTotal() % searchParam.getPageSize() :
//                        searchResponseVo.getTotal() / searchParam.getPageSize() + 1;
        //总结公式
        Long totalPages = (searchResponseVo.getTotal() + searchParam.getPageSize() - 1) / searchParam.getPageSize();
        searchResponseVo.setTotalPages(totalPages);


        return searchResponseVo;
    }

    /**
     * 构建es 动态生成dsl语句
     *
     * @param searchParam
     * @return
     */
    private SearchRequest buildQueryDsl(SearchParam searchParam) {
        //声明查询器 {}
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // 声明一个QueryBuild对象 {query bool}
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        //1判断用户是根据分类id过滤 还是 直接关键字检索查询
        if (!StringUtils.isEmpty(searchParam.getCategory1Id())) {
            //通过一级分类id过来的
            // {query bool filter term}
            boolQueryBuilder.filter(QueryBuilders.termQuery("category1Id", searchParam.getCategory1Id()));
        }
        if (!StringUtils.isEmpty(searchParam.getCategory2Id())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("category2Id", searchParam.getCategory2Id()));
        }
        if (!StringUtils.isEmpty(searchParam.getCategory3Id())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("category3Id", searchParam.getCategory3Id()));
        }

        //2判断用户通过关键字进行检索
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            //通过关键词检索 {query match}
            MatchQueryBuilder title = QueryBuilders.matchQuery("title", searchParam.getKeyword()).operator(Operator.AND);

            //如果有过滤同时还检索? {query bool must match}
            boolQueryBuilder.must(title);

            //如果关键词不为空 可能设置高亮
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("title");
            highlightBuilder.preTags("<span style=color:red>");
            highlightBuilder.postTags("</span>");
            searchSourceBuilder.highlighter(highlightBuilder);
        }

        //3根据品牌id过滤  前台传来数据 trademark=1:小米  传递的参数直接映射到实体类
        String trademark = searchParam.getTrademark();
        if (!StringUtils.isEmpty(trademark)) {
            //进行品牌id过滤
            String[] split = trademark.split(":");
            if (split != null && split.length == 2) {
                // {query bool filter term}
                boolQueryBuilder.filter(QueryBuilders.termQuery("tmId", split[0]));
            }
        }

        //4根据平台属性值过滤  &props=23:6G:运行内存&props=24:256G:机身内存
        String[] props = searchParam.getProps();
        if (props != null && props.length > 0) {
            for (String prop : props) {
                // 23:6G:运行内存 = prop
                // 构建过滤查询
                String[] split = prop.split(":");
                if (split != null && split.length == 3) {
                    //创建最内层boolQuery
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    BoolQueryBuilder subQuery = QueryBuilders.boolQuery();
                    subQuery.filter(QueryBuilders.termQuery("attrs.attrId", split[0]));
                    subQuery.filter(QueryBuilders.termQuery("attrs.attrValue", split[1]));
                    //subQuery.must(QueryBuilders.termQuery("attrs.attId",split[0]));
                    //subQuery.must(QueryBuilders.termQuery("attrs.attrValue",split[1]));

                    //写入nested
                    boolQuery.must(QueryBuilders.nestedQuery("attrs", subQuery, ScoreMode.None));

                    //写入最外层的boolQuery
                    boolQueryBuilder.filter(boolQuery);
                }
            }
        }
        searchSourceBuilder.query(boolQueryBuilder);

        //5 分页检索数据
        //( 当前页 -1 ) * 每页记录数 = 起始页
        int from = (searchParam.getPageNo() - 1) * searchParam.getPageSize();
        searchSourceBuilder.from(from);//从第几条数据开始查询
        searchSourceBuilder.size(searchParam.getPageSize());

        //6 排序  价格  order=2:desc  order=2:asc  综合 order=1:desc order=1:asc
        String order = searchParam.getOrder();
        if (!StringUtils.isEmpty(order)) {
            String[] split = order.split(":");
            String field = "";
            if (split != null && split.length == 2) {
                switch (split[0]) {
                    case "1":
                        field = "hotScore";
                        break;
                    case "2":
                        field = "price";
                        break;
                }
                searchSourceBuilder.sort(field, "asc".equals(split[1]) ? SortOrder.ASC : SortOrder.DESC);
            } else {
                //默认排序规则
                searchSourceBuilder.sort("hotScore", SortOrder.DESC);
            }
        }

        //7 品牌聚合检索
        searchSourceBuilder.aggregation(AggregationBuilders.terms("tmIdAgg").field("tmId")
                .subAggregation(AggregationBuilders.terms("tmNameAgg").field("tmName"))
                .subAggregation(AggregationBuilders.terms("tmLogoUrlAgg").field("tmLogoUrl"))
        );

        //8 平台属性聚合  nested类型
        searchSourceBuilder.aggregation(AggregationBuilders.nested("attrAgg", "attrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue"))
                )
        );

        //设置好索引库  GET /goods/_search
        SearchRequest searchRequest = new SearchRequest("goods");
        //将{}添加到searchRequest对象中
        searchRequest.source(searchSourceBuilder);

        // 过滤字段用 需要的字段数据 "id","defaultImage","title","price","createTime"
        searchSourceBuilder.fetchSource(new String[]{"id", "defaultImg", "title", "price", "createTime"}, null);

        //通过打印的方式 获取DSL语句
        System.out.println("DSL:\t" + searchSourceBuilder.toString());

        return searchRequest;
    }

    /**
     * 转换结果集方法  searchResponse --> searchResponseVo
     *
     * @param searchResponse
     * @return
     */
    private SearchResponseVo parseSearchResult(SearchResponse searchResponse) {
        /*
            private List<SearchResponseTmVo> trademarkList;
            private List<SearchResponseAttrVo> attrsList = new ArrayList<>();
            private List<Goods> goodsList = new ArrayList<>();
            private Long total;//总记录数
         */

        // private Long total;//总记录数
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        SearchHits hits = searchResponse.getHits();
        long value = hits.getTotalHits().value;
        searchResponseVo.setTotal(value);

        // private List<Goods> goodsList = new ArrayList<>();
        ArrayList<Goods> goodsList = new ArrayList<>();
        SearchHit[] subHits = hits.getHits();
        if (subHits != null && subHits.length > 0) {
            for (SearchHit subHit : subHits) {
                String sourceAsString = subHit.getSourceAsString();
                //字符串转换为Goods
                Goods goods = JSON.parseObject(sourceAsString, Goods.class);

                //细节:  在获取到数据时,判断数据中是否有高亮部分
                if (subHit.getHighlightFields().get("title") != null) {
                    //覆盖原来的title
                    Text title = subHit.getHighlightFields().get("title").getFragments()[0];
                    goods.setTitle(title.toString());
                }

                //goods添加到集合中
                goodsList.add(goods);
            }
        }
        searchResponseVo.setGoodsList(goodsList);

        // private List<SearchResponseTmVo> trademarkList;
//        ArrayList<SearchResponseTmVo> trademarkList = new ArrayList<>();
        Map<String, Aggregation> aggregationMap = searchResponse.getAggregations().asMap();
        ParsedLongTerms tmIdAgg = (ParsedLongTerms) aggregationMap.get("tmIdAgg");
        List<SearchResponseTmVo> trademarkList = tmIdAgg.getBuckets().stream().map(bucket -> {
            SearchResponseTmVo searchResponseTmVo = new SearchResponseTmVo();

            String keyAsString = bucket.getKeyAsString();
            searchResponseTmVo.setTmId(Long.parseLong(keyAsString));

            ParsedStringTerms tmNameAgg = bucket.getAggregations().get("tmNameAgg");
            String tmName = tmNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmName(tmName);

            ParsedStringTerms tmLogoUrlAgg = bucket.getAggregations().get("tmLogoUrlAgg");
            String tmLogoUrl = tmLogoUrlAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmLogoUrl(tmLogoUrl);

            return searchResponseTmVo;
        }).collect(Collectors.toList());

        searchResponseVo.setTrademarkList(trademarkList);

        // private List<SearchResponseAttrVo> attrsList = new ArrayList<>();
//        ArrayList<SearchResponseAttrVo> attrsList = new ArrayList<>();
        ParsedNested attrAgg = (ParsedNested) aggregationMap.get("attrAgg");
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attrIdAgg");
        List<SearchResponseAttrVo> attrsList = attrIdAgg.getBuckets().stream().map(bucket -> {
            SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
            String keyAsString = bucket.getKeyAsString();
            searchResponseAttrVo.setAttrId(Long.parseLong(keyAsString));

            ParsedStringTerms attrNameAgg = bucket.getAggregations().get("attrNameAgg");
            String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseAttrVo.setAttrName(attrName);

            // 平台属性值是集合
            ParsedStringTerms attrValueAgg = bucket.getAggregations().get("attrValueAgg");
            List<String> valueList = attrValueAgg.getBuckets().stream().map(item -> {
                return item.getKeyAsString();
            }).collect(Collectors.toList());
            searchResponseAttrVo.setAttrValueList(valueList);

            return searchResponseAttrVo;
        }).collect(Collectors.toList());

        //返回封装后的数据
        return searchResponseVo;
    }
}
