package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.list.SearchParam;
import com.google.errorprone.annotations.CompileTimeConstant;
import com.sun.javafx.collections.MappingChange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: ListController
 * @author: javaermamba
 * @date: 2023-06-2023/6/17-15:03
 * @Description: es检索数据控制器
 */
@Controller
public class ListController {

    @Qualifier("com.atguigu.gmall.list.client.ListFeignClient")
    @Autowired
    private ListFeignClient listFeignClient;

    //  http://list.gmall.com/list.html?category3Id=61
    //  http://list.gmall.com/list.html?keyword=小米手机
    //  springmvc 对象传值的时候： 实体类的属性名 与 ? 后面的参数名称一致. 所以可以直接用对象接收
    @GetMapping("list.html")
    public String list(SearchParam searchParam , Model model){
        // 需要做远程调用
        // class 和map是可以互相替换的
        Result<Map> result = listFeignClient.searchList(searchParam);

        //编写方法获取urlParam
        // 记录用户查询条件
        String urlParam = this.makeUrlParm(searchParam);
        //制作品牌面包屑
        // 传递的数据 trademark=1:小米 通过切分 取小米就可以了,所以下面的直接传入该参数即可
        String trademarkParam = this.makeTrademarkParam(searchParam.getTrademark());
        //制作平台属性面包屑  平台属性数据. 23:4G:运行内存 一个或多个。
        // 可以看做返回一个实体类: 实体类中必须有以下三个属性:  attrId attrValue attrName
        // 还可以返回一个map key分别是: attrId attrValue attrName
        List<Map> propsParamList = this.makePropsParamList(searchParam.getProps());
        // 制作排序orderMap 必须的属性参数是 type sort
        // 用户点击排序传入的数据 order=2:desc
        Map<String,Object> orderMap = this.makeOrderMap(searchParam.getOrder());

        //存储数据 供前端展示
        model.addAttribute("searchParam",searchParam);
        // urlParam参数 : 拼接用户查询条件的
        model.addAttribute("urlParam",urlParam);
        // trademarkParam : 品牌面包屑
        model.addAttribute("trademarkParam",trademarkParam);
        // propsParamList: 平台属性面包屑
        model.addAttribute("propsParamList",propsParamList);
        // orderMap 排序
        // 要么使用map存储数据,要么使用实体类存储数据  必须的属性参数是 type sort
        model.addAttribute("orderMap",orderMap);


        // 根据前端的参数接收类型,可以看出,
        // 后台应该存储: searchParam参数,trademarkParam参数,urlParam参数,propsParamList,trademarkList,attrsList,orderMap,goodsList，pageNo，totalPages
        // 我们后端的对象SearchResponseVo对象result.getData(); 获取到的对象 SearchResponseVo trademarkList,attrsList,goodsList,pageNo,totalPages
        // class 和map是可以互相替换的
        model.addAllAttributes(result.getData());

        //返回检索页面视图名称
        return "list/index";
    }


    //制作orderMap排序   order=2:des
    // 排序： type 表示的是 综合，还是价格  sort 表示的是排序规则.
    private Map<String, Object> makeOrderMap(String order) {
        HashMap<String, Object> map = new HashMap<>();
        //map中有两个key type sort 这两个参数是什么,完全看传入进来的数据是什么(也就是用户使用的是那种排序方式)
        //判断传入是否为空,也就是用户是否使用了排序
        if (!StringUtils.isEmpty(order)){
            //使用了排序,则使用分割
            String[] split = order.split(":");
            //判断传入数据是否符合规则 order=2:des
            map.put("type",split[0]);
            map.put("sort",split[1]);
        }else {
            //没有选择,使用默认排序
            map.put("type","1");
            map.put("sort","desc");
        }

        return map;
    }

    // 制作平台属性面包屑  &props=24:128G:机身内存&props=23:8G:运行内存
    private List<Map> makePropsParamList(String[] props) {
        //声明集合
        ArrayList<Map> mapList = new ArrayList<>();
        //判断用户是否点击了平台属性值进行了过滤
        if (props!=null&&props.length>0){
            //循环遍历集合中的数据
            for (String prop : props) {
                String[] split = prop.split(":");
                //判断分割完后的数据
                if (split!=null && split.length==3){
                    HashMap<String, Object> hashMap = new HashMap<>();
                    hashMap.put("attrId",split[0]);
                    hashMap.put("attrValue",split[1]);
                    hashMap.put("attrName",split[2]);
                    //将map添加到集合里
                    mapList.add(hashMap);
                }
            }
        }
        //返回数据
        return mapList;
    }

    // 品牌面包屑
    private String makeTrademarkParam(String trademark) {
        if (!StringUtils.isEmpty(trademark)){
            String[] split = trademark.split(":");
            //获取品牌名称 trademark=1:小米
            if (split!=null && split.length==2){
                return "品牌"+split[1];
            }
        }
        return null;
    }


    //记录用户的点击条件参数
    private String makeUrlParm(SearchParam searchParam) {
        //声明对象,记录拼接的url
        StringBuilder stringBuilder = new StringBuilder();

        //  https://list.jd.com/list.html?cat=9987,2C653,2C655&ev=13743_42587%5E&cid3=655
        //  http://sph-list.atguigu.cn/list.html?category3Id=61&props=24:256G:%E6%9C%BA%E8%BA%AB%E5%86%85%E5%AD%98&order=
        //判断用户是否根据分类id检索
        //  http://sph-list.atguigu.cn/list.html?category1Id=2
        if (!StringUtils.isEmpty(searchParam.getCategory1Id())){
            //说明用户点击了一级分类id进来的,我们就把一级分类id拼上
            stringBuilder.append("category1Id=").append(searchParam.getCategory1Id());
        }
        if (!StringUtils.isEmpty(searchParam.getCategory1Id())){
            //说明用户点击了一级分类id进来的,我们就把二级分类id拼上
            stringBuilder.append("category2Id=").append(searchParam.getCategory2Id());
        }
        if (!StringUtils.isEmpty(searchParam.getCategory1Id())){
            //说明用户点击了一级分类id进来的,我们就把三级分类id拼上
            stringBuilder.append("category3Id=").append(searchParam.getCategory3Id());
        }

        // 通过关键词检索进来
        //  http://sph-list.atguigu.cn/list.html?keyword=手机
        if (!StringUtils.isEmpty(searchParam.getKeyword())){
            //说明用户根据关键词检索进入
            stringBuilder.append("keyword=").append(searchParam.getKeyword());
        }

        // 通过品牌进行过滤进入
        //  http://sph-list.atguigu.cn/list.html?keyword=手机&trademark=1:小米
        if (!StringUtils.isEmpty(searchParam.getTrademark())){
            if (stringBuilder.length()>0){
                //说明用户点击了品牌项(这一项只能追加:即,要么是分类id过来的,要么是关键字过来的)才可以在url路径上拼接这一项
                stringBuilder.append("&trademark=").append(searchParam.getTrademark());
            }
        }

        // 平台属性过滤进入
        // http://sph-list.atguigu.cn/list.html?keyword=手机&trademark=1:小米&props=24:256G:机身内存&props=23:4G:运行内存
        //先拿到平台属性的数组
        String[] props = searchParam.getProps();
        //再判断是否点击了
        if (props!=null&&props.length>0){
            //循环遍历
            for (String prop : props) {
                //判断stringBuilder是否有参数,有参数说明点击了,就把点击的追加到url中
                if (stringBuilder.length()>0){
                    stringBuilder.append("&props=").append(prop);
                }
            }
        }

        //返回url路径
        return "list.html?"+stringBuilder.toString();
    }
}
