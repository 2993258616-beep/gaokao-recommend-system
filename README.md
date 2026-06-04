# 河南高考志愿推荐系统 2026预测版

## 一、项目说明

这个项目已经按你的要求做好：

- Java 8 可运行
- Spring Boot 2.7.18
- 默认端口：8081
- 默认数据库名：gaokao_recommend_db
- 默认使用 H2 文件数据库，IDEA 直接运行即可
- 含登录功能
- 含基础网安防护：登录认证、BCrypt 密码加密、Session 防护、单账号会话限制、安全响应头、限流、防错误堆栈泄露
- “热门专业方向”已经改为“专业名称”
- 专业名称下拉框来自你表格 all_database_unique_majors.csv 中的 major 列
- 地区筛选使用 school_province，也就是学校所在省份，例如河南、北京、江苏、浙江等
- province 字段固定代表河南考生，本系统逻辑是“河南考生在外省/各省院校的预测分数线”

## 二、直接运行方法

1. 用 IDEA 打开本文件夹：`gaokao-recommend-system`
2. 等 Maven 自动下载依赖
3. 找到启动类：

```text
src/main/java/com/gaokao/recommend/GaokaoRecommendApplication.java
```

4. 右键运行
5. 浏览器打开：

```text
http://localhost:8081
```

## 三、登录账号

```text
账号：admin
密码：admin123
```

密码启动时会自动用 BCrypt 加密写入数据库，不是明文密码。

## 四、数据库说明

默认数据库文件在：

```text
data/gaokao_recommend_db.mv.db
```

核心表：

```text
user_account        登录账号表
major_name_stat     专业名称表，对应你表格里的 major/count/subject_types
prediction_line     河南考生预测分数线表
```

## 五、如果你要改成 MySQL

先创建数据库：

```sql
CREATE DATABASE gaokao_recommend_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

然后修改：

```text
src/main/resources/application-mysql.properties
```

把 MySQL 用户名密码改成你的：

```properties
spring.datasource.username=root
spring.datasource.password=123456
```

启动时加参数：

```text
--spring.profiles.active=mysql
```

注意：MySQL 模式默认不自动建表，真实部署时可以把 `src/main/resources/db/schema.sql` 放到 MySQL 中执行。

## 六、真实数据导入要求

你真实表字段建议严格使用下面这些：

```text
predict_year
province
school_code
school_name
subject_type
major_group
major_group_full
school_type
school_level
school_nature
plan_count
filing_score
range_float
predict_score
predict_low
predict_high
predict_range
major_direction
major_category
school_province
confidence
```

其中：

- province：河南，代表河南考生
- school_province：学校地区，例如北京、江苏、浙江、广东等
- major_direction：专业名称/专业方向
- major_category：专业大类
- confidence：冲刺、稳妥、保底

## 七、你最容易改错的地方

前端显示叫：

```text
专业名称
```

后端字段叫：

```text
majorName
```

数据库专业名称字段叫：

```text
major_name
```

不要再写：

```text
热门专业方向
hotMajorDirection
```
