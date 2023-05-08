# gmall
1. 核心技术
SpringBoot：简化新Spring应用的初始搭建以及开发过程
SpringCloud：基于Spring Boot实现的云原生应用开发工具
  SpringCloud使用的技术：（Spring Cloud Gateway、Spring Cloud Alibaba Nacos、Spring Cloud Alibaba Sentinel、Spring Cloud Task和Spring Cloud Feign等）
MyBatis-Plus：持久层框架
Redis：内存做缓存
Redisson：基于redis的Java驻内存数据网格
RabbitMQ：消息中间件
ElasticSearch+Kibana+Logstash: 全文检索服务器+可视化数据监控+日志收集框架
ThreadPoolExecutor：线程池来实现异步操作，提高效率
Thymeleaf: 页面模板技术
Swagger2/POSTMAN/Yapi：Api接口文档工具
Minio：分布式文件存储类似于OSS，FastDFS
支付宝支付：alipay.com
Mysql：关系型数据库 {mycat/sharding-jdbc 进行分库，分表}
Lombok: 实体类的中get，set 生成的jar包
Ngrok/natapp：内网穿透
Docker：容器技术
Git：代码管理工具
DockerFile：管理Docker镜像命令文本
Jenkins：持续集成工具
Vue.js：web 界面的渐进式框架
Node.js： JavaScript 运行环境
NPM：包管理器

2. 需要掌握的解决方案
 分布式架构、缓存管理、分布式事务、单点登录、商品后台管理、文件管理系统

3. 说明：
1、	建议内存8个G以上
2、	培养自己独立阅读代码的能力
3、	分析 解构业务需求
4、	新的知识点，难点敲

4. 项目架构
![image](https://user-images.githubusercontent.com/130919429/236773293-f21cb3f7-815f-42fa-97a4-8b8e3ad7aa95.png)

5. 业务简介
![image](https://user-images.githubusercontent.com/130919429/236773518-c554bba4-fd55-49f8-8408-95a98aef30ae.png)

首页	      静态页面，包含了商品分类，搜索栏，商品广告位。
全文搜索	  通过搜索栏填入的关键字进行搜索，并列表展示
分类查询	  根据首页的商品类目进行查询
商品详情	  商品的详细信息展示
购物车	     将有购买意向的商品临时存放的地方
单点登录	  用户统一登录的管理
结算	      将购物车中勾选的商品初始化成要填写的订单
下单	      填好的订单提交
支付服务	  下单后，用户点击支付，负责对接第三方支付系统。
订单服务	  负责确认订单是否付款成功，并对接仓储物流系统。
仓储物流	  独立的管理系统，负责商品的库存。
后台管理	  主要维护类目、商品、库存单元、广告位等信息。
秒杀	      秒杀抢购完整方案

6. 电商软件环境安装(查看安装软件是否可用)
nacos: http://192.168.43.33:8848/nacos
sentinel: http://192.168.43.33:8858/#/dashboard
用户名：密码：sentinel
mysql 用户名:root ，密码：root
rabbitmq: http://192.168.43.33:15672/#/ 用户名，密码 guest 
es: http://192.168.43.33:9200/
kibana：http://192.168.43.33:5601/app/kibana
zikpkin: http://192.168.43.33:9411/zipkin/
Minio：http://192.168.43.33:9001/buckets

7. 说明
配置hosts环境
作用: 能够直接修改C:\Windows\System32\drivers\etc\hosts 文件！
修改系统hosts文件

# gmall
192.168.200.1 file.service.com www.gmall.com item.gmall.com 
192.168.200.1 activity.gmall.com passport.gmall.com cart.gmall.com list.gmall.com 
192.168.200.1 order.gmall.com payment.gmall.com api.gmall.com comment.gmall.com

