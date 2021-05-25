# 监控数据持久化

1. 基于官方提供的MetricsRepository类基础上，实现了TaosDBMetricsRepository这个类
2. 我没有用influxDb来做存储，是因为它集群版本要收费......
3. 我用的国产的一款开源时序数据库TDengine,github上star数有10多k,而且集群版本开源,真不错。项目地址  [TDengine](https://github.com/taosdata/TDengine)
4. TaosDBMetricsRepository类下有四个方法，单个保存和批量保存，还有查询列表方法
5. 生产中用，应该还要对时序数据库做集群
6. 操作时序数据库用的语句需要改造，但大体是这样
7. TDengine的安装查看它官网文档就好了，在jdbc操作它，直接看sentinel-dashboard包下的pom文件，我已经引入了
8. 如果你想拿来即用，那么你安装好时序数据库后，改改sentinel-dashboard的yml配置文件，数据库地址改一下，然后TaosDBMetricsRepository类里面的保存数据方法，我指定了一个表名，看你自己喜欢不，不喜欢可以改掉。
8. 本项目基于 https://gitee.com/hhjiesen/sentinel-rule-nacos?_from=gitee_search 改造而来，十分感谢
9. 数据建表sql,这是TDengine里的超级表,查询并不是查这个名字的
```
CREATE STABLE meters (ts TIMESTAMP,id INT,gmtcreate TIMESTAMP,gmtmodified TIMESTAMP,app NCHAR ( 50 ),_timestamp TIMESTAMP,resource NCHAR ( 50 ),passqps INT,successqps INT,blockqps INT,exceptionqps INT,rt DOUBLE,count INT,resourcecode INT ) TAGS (_version NCHAR ( 10 ));、
```
