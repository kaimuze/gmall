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
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
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
     * 转换结果集方法
     *   将上面查询到的数据对象searchResponse,进行数据转换.转换为我们需要的数据类型SearchResponseVo
     *   即: searchResponse ----> searchResponseVo
     * @param searchResponse
     * @return
     */
    private SearchResponseVo parseSearchResult(SearchResponse searchResponse) {
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        /*
         赋值这些字段:
         SearchResponseVo类中的属性就是这些,我们要做的就是给这写属性赋上值即可
         private List<SearchResponseTmVo> trademarkList; 品牌
         private List<SearchResponseAttrVo> attrsList = new ArrayList<>(); 平台属性
         private List<Goods> goodsList = new ArrayList<>(); //商品
         private Long total;//总记录数
         */

        // 总记录数total赋值
        // 总记录数在es库执行完插叙操作的返回结果集中的hits中
        SearchHits hits = searchResponse.getHits();
        long value = hits.getTotalHits().value;
        searchResponseVo.setTotal(value);

        // 赋值goodsList
        // 创建一个封装goods的空集合集合,再将goods数据封装到集合中,最后赋值给GoodsList
        ArrayList<Goods> goodsList = new ArrayList<>();
        //集合中每一个存储的数据类型是Goods,所以要给Goods赋值,数据在hits/hits/source中
        SearchHit[] subHits = hits.getHits();
        if (subHits != null && subHits.length > 0){
            //循环遍历
            for (SearchHit subHit : subHits) {
                //获取到hits/hits/的每一个_source对象
                String sourceAsString = subHit.getSourceAsString();
                // 将字符串转换为Goods
                Goods goods = JSON.parseObject(sourceAsString, Goods.class);

                //细节: 如果用户通过关键词检索,则需要获取到高亮的商品名称
                if (subHit.getHighlightFields().get("title")!=null){
                    //如果不为空,说明有高亮显示.要将原来的title覆盖
                    Text title = subHit.getHighlightFields().get("title").getFragments()[0];
                    goods.setTitle(title.toString());
                }
                //将每一次循环得到的_source对象(上面一步已经转换为了goods对象类型)都放到集合里
                goodsList.add(goods);
            }
        }
        //商品集合赋值完成
        searchResponseVo.setGoodsList(goodsList);

        // 赋值品牌数据
        //List<SearchResponseTmVo> trademarkList = new ArrayList<>();
        // trademarkList空数据 ,需要赋值
        // 获取聚合中对应的品牌数据 转换成map的key-value形式,方便获取
        Map<String, Aggregation> aggregationMap = searchResponse.getAggregations().asMap();
        //获取品牌id的聚合对象tmIdAgg
        //Aggregation tmIdAgg = aggregationMap.get("tmIdAgg");
        // 再接着向下直接获取不道bucket对象,因为返回的类Aggregation相对比较大所以上面需要将类强转为较小的类
        ParsedLongTerms tmIdAgg = (ParsedLongTerms) aggregationMap.get("tmIdAgg");
        //现在就可以继续向下调用,找到bucket了(bucket是一个集合: 使用stream流遍历)
        List<SearchResponseTmVo> trademarkList = tmIdAgg.getBuckets().stream().map(bucket -> {
            //创建一个对象,接收流中bucket集合数据
            SearchResponseTmVo searchResponseTmVo = new SearchResponseTmVo();
            // 为searchResponseTmVo的每一个属性赋值
            // 获取到品牌id "key":1
            String keyAsString = ((Terms.Bucket)bucket).getKeyAsString();
            searchResponseTmVo.setTmId(Long.parseLong(keyAsString));

            //获取到品牌名称,tmNameAgg是获取id的子聚合,不能直接get得到
            //Aggregation tmNameAgg = bucket.getAggregations().get("tmNameAgg");
            //并且还需要强转为小类才能获取到
            ParsedStringTerms tmNameAgg = ((Terms.Bucket)bucket).getAggregations().get("tmNameAgg");
            String tmName = tmNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmName(tmName);

            // 获取品牌的logoUrl
            ParsedStringTerms tmLogoUrlAgg = ((Terms.Bucket) bucket).getAggregations().get("tmLogoUrlAgg");
            String tmLogoUrl = tmLogoUrlAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmLogoUrl(tmLogoUrl);

            //返回品牌对象
            return searchResponseTmVo;
        }).collect(Collectors.toList());
        searchResponseVo.setTrademarkList(trademarkList);

        // 赋值平台属性 es中的数据结果集类型是nested类型,需要转换
        //List<SearchResponseAttrVo> attrsList = new ArrayList<>();
        //获取到数据 attrAgg查询DSL语句中的数据类型是nested,而查询到的结果集不是该类型,是map,所以下面需要转换为nested类型
        ParsedNested attrAgg = (ParsedNested) aggregationMap.get("attrAgg");
        //这一步取到 aggs
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attrIdAgg");
        // 获取到bucket集合,使用stream流 封装数据
        List<SearchResponseAttrVo> attrsList = attrIdAgg.getBuckets().stream().map(bucket -> {
            //创建对象
            SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
            //赋值平台属性id
            String keyAsString = ((Terms.Bucket)bucket).getKeyAsString();
            searchResponseAttrVo.setAttrId(Long.parseLong(keyAsString));

            //赋值平台属性名
            ParsedStringTerms attrNameAgg = ((Terms.Bucket)bucket).getAggregations().get("attrNameAgg");
            String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseAttrVo.setAttrName(attrName);

            //赋值平台属性值名 不是一个数据了,因为一个属性下 对应多个属性值名
            ParsedStringTerms attrValueAgg = ((Terms.Bucket)bucket).getAggregations().get("attrValueAgg");
            // 将attrValueAgg集合中的每一个key都拿出来
            // Terms.Bucket::getKeyAsString表示根据key获取到数据
            List<String> attrValueList = attrValueAgg.getBuckets().stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
//            //第二种方法: 普通for循环
//            ArrayList<String> strings = new ArrayList<>();
//            for (Terms.Bucket attrValueAggBucket : attrValueAgg.getBuckets()) {
//                String attrValue = attrValueAggBucket.getKeyAsString();
//                strings.add(attrValue);
//            }
//            searchResponseAttrVo.setAttrValueList(strings);

            searchResponseAttrVo.setAttrValueList(attrValueList);

            //返回数据
            return searchResponseAttrVo;
        }).collect(Collectors.toList());

        searchResponseVo.setAttrsList(attrsList);

        //返回对象
        return searchResponseVo;
    }
}
