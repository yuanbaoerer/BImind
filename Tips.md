Tips

- 前端和后端链接的网络服务代码，可以使用前端的openapi插件一键生成，会在Service/controller中生成



## 不解之处

**SpringBoot**

1. @Component：标记组件的类
   1. spring中的组件是什么？
2. @Repository：标记为数据访问层的类
   1. 数据访问层是哪一部分？

**Git**

1. Git中的main、branch分支是做什么的？



## 安全性

如果用户上传一个超大的文件怎么办？比如 1000G？

- 只要涉及到用户自主上传的操作，一定要检验文件（图像）
  - 文件的大小
  - 文件的后缀
  - 文件的内容（成本要高一些）
  - 文件的合规性（比如敏感内容，建议使用第三方的审核功能）扩展点：接入腾讯云的图片万象数据审核（COS对象存储的审核功能）

## 数据存储优化

我们把每个图表的原始数据全部存放在了同一个数据表(chart表)的字段里，这种设计虽然方便了数据的获取和管理，但是在系统后期数据量大的情况下，会面临一些隐藏的优化问题。例如，如果允许用户上传100 兆(100 MB)的原始数据那么每一个图表、每一行数据都会在该字段中存储100 兆的数据。如果有 1000个用户，每个用户有 100 个图表，那这个数据表的大小将非常巨大，从而导致查询图表或查询 chart表等操作变得缓慢。
**现状: **我们把每个图表的原始数据全部存放在了同一个数据表(chart表)的字段里。

问题:
1.如果用户上传的原始数据量很大、图表数日益增多，查询 chart表就会很慢2.对于 BI 平台，用户是有查看原始数据、对原始数据进行简单查询的需求的。现在如果把所有数据存放在一个字段(列)中，查询时，只能取出这个列的所有内容。
**补充:**比如下图这张表，我只想要 xy 这两列，就要先把所有的原始数据查出来，然后再去做过滤。

### **分库分表**

**就是每一个文件的原始数据存储时，都新建一个chart_(图表id)的table，而不是往一个table里面加一条数据**

- 分开存储

  ```sql
  -- auto-generated definition
  create table chart_1659210482555121666
  (
    日期  int null,
    用户数 int null
  );
  
  ```

- 分开查询：以前直接查询图表，取chartdata字段；现在改为读取chart_{图表id}的数据表

  ```sql
  select * from chart_{1659210482555121666};
  ```

> [!IMPORTANT]
>
> 分库分表的思路
> 在数据库设计中考虑使用分库分表的思路可以有效地解决大数据量和高并发的问题。可以分**水平分表**和**垂直分库**两种方式。水平分表指在数据量大的情况下，将表按照某个字段的值进行拆分和分散存储例如拆分出前1万个用户一个表，后1万个用户一个表。垂直分库则是将不同的业务按照相关性进行划分，例如将用户中心用户相关的内容划分到一个库中，订单、支付信息和订单相关的划分到另一个库中，从而提高系统的可扩展性和稳定性，
> 分库分表是数据库设计中重要的一部分，能有效地优化系统的性能，提高用户体验，也是一个优秀的简历亮点。

### 水平分表（Horizontal Sharding）和垂直分库（Vertical Partitioning）

水平分表和垂直分库是数据库分库分表策略中的两种常见方法，它们分别针对不同的问题和场景。以下是对这两种策略的详细介绍：

### 1. **水平分表（Horizontal Sharding）**

水平分表是将数据分散到多个表中，每个表的结构相同，但存储的数据不同。这种策略通常用于解决单表数据量过大导致的性能问题。

#### **适用场景**

- **数据量大**：单表数据量过大，导致查询性能下降。
- **高并发**：需要支持高并发读写操作，单表无法满足性能需求。
- **分布式存储**：需要将数据分散到多个节点上，以实现水平扩展。

#### **实现方式**

- **按范围分表**：根据某个字段的范围将数据分散到不同的表中。例如，按时间戳分表，将不同时间段的数据存储在不同的表中。
- **按哈希分表**：根据某个字段的哈希值将数据分散到不同的表中。例如，按用户 ID 的哈希值分表。
- **按业务逻辑分表**：根据业务逻辑将数据分散到不同的表中。例如，将不同类型的订单存储在不同的表中。

#### **示例**

假设有一个用户表 `users`，数据量非常大，我们决定按用户 ID 的哈希值进行水平分表。

java复制

```java
public class UserDAO {
    private static final String TABLE_PREFIX = "users_";

    public void insertUser(User user) {
        int shardId = user.getId() % 10; // 假设我们分 10 个表
        String table = TABLE_PREFIX + shardId;
        // 执行插入操作
        // ...
    }

    public User getUserById(int userId) {
        int shardId = userId % 10; // 假设我们分 10 个表
        String table = TABLE_PREFIX + shardId;
        // 执行查询操作
        // ...
    }
}
```

### 2. **垂直分库（Vertical Partitioning）**

垂直分库是将数据分散到多个数据库实例中，每个数据库实例存储不同的字段或不同的业务逻辑的数据。这种策略通常用于解决单个数据库实例的性能瓶颈和存储瓶颈。

#### **适用场景**

- **字段过多**：单表字段过多，导致查询性能下降。
- **业务逻辑复杂**：不同的业务逻辑需要存储不同类型的数据。
- **分布式存储**：需要将数据分散到多个节点上，以实现水平扩展。

#### **实现方式**

- **按字段分库**：将不同字段的数据分散到不同的数据库实例中。例如，将用户的基本信息存储在一个数据库实例中，将用户的订单信息存储在另一个数据库实例中。
- **按业务逻辑分库**：根据业务逻辑将数据分散到不同的数据库实例中。例如，将用户信息存储在一个数据库实例中，将订单信息存储在另一个数据库实例中。

#### **示例**

假设有一个用户表 `users` 和一个订单表 `orders`，我们决定将用户信息和订单信息分别存储在不同的数据库实例中。

java复制

```java
public class UserDAO {
    private static final String USER_DB = "user_db";
    private static final String ORDER_DB = "order_db";

    public void insertUser(User user) {
        // 插入用户信息到用户数据库
        // ...
    }

    public void insertOrder(Order order) {
        // 插入订单信息到订单数据库
        // ...
    }

    public User getUserById(int userId) {
        // 查询用户信息
        // ...
    }

    public Order getOrderById(int orderId) {
        // 查询订单信息
        // ...
    }
}
```

### **总结**

- **水平分表**：将数据分散到多个表中，每个表的结构相同，但存储的数据不同。适用于解决单表数据量过大和高并发问题。
- **垂直分库**：将数据分散到多个数据库实例中，每个数据库实例存储不同的字段或不同的业务逻辑的数据。适用于解决单个数据库实例的性能瓶颈和存储瓶颈。

通过合理选择水平分表和垂直分库的策略，可以显著提高系统的性能和可扩展性。

### 实现水平分表——MyBatis的动态SQL

想清楚哪些是需要动态替换的，比如查询的数据表名chart_{图表id}.

1. 在ChartMapper.xml 中定义sql语句

   ```xml
   <!--  
   queryChartData唯一标识符;parameterType查询语句的参数类型,类型为字符串;
   resultType查询结果的返回类型,类型为map类型;
   ${querySql}是SQL查询语句的占位符;
   select * from chart_#{chartId} 不够灵活,${querySql}是最灵活的方式，
   就是把sql语句完全交给程序去传递，有一定的风险;
   一旦使用$符号，就有sql注入的风险。
   -->
   <select id="queryChartData" parameterType="string" resultType="map">
     ${querySql}
   </select>
   <!-- 
   可以在程序里面去做校验。只要保证这个SQL是通过你的后端生成的，
   在生成的过程中做了校验，就不会有这种漏洞的风险。 
   -->
   
   ```

2. 在ChartMapper中添加对应id的语句

   ```java
   package com.yupi.springbootinit.mapper;
   
   import com.yupi.springbootinit.model.entity.Chart;
   import com.baomidou.mybatisplus.core.mapper.BaseMapper;
   import java.util.List;
   import java.util.Map;
   
   /**
    * @Entity com.yupi.springbootinit.model.entity.Chart
    */
   public interface ChartMapper extends BaseMapper<Chart> {
      /*
       * 方法的返回类型是 List<Map<String, Object>>,
       * 表示返回的是一个由多个 map 组成的集合,每个map代表了一行查询结果，
       * 并将其封装成了一组键值对形式的对象。其中,String类型代表了键的类型为字符串，
       * Object 类型代表了值的类型为任意对象,使得这个方法可以适应不同类型的数据查询。
       *
       */
       List<Map<String, Object>> queryChartData(String querySql);
   }
   ```

3. 测试

   ```java
   package com.yupi.springbootinit.mapper;
   
   import org.junit.jupiter.api.Test;
   import org.springframework.boot.test.context.SpringBootTest;
   
   import javax.annotation.Resource;
   
   import java.util.List;
   import java.util.Map;
   
   import static org.junit.jupiter.api.Assertions.*;
   
   @SpringBootTest
   class ChartMapperTest {
   
       @Resource
       private ChartMapper chartMapper;
   
       @Test
       void queryChartData() {
           String chartId = "1659210482555121666";
           String querySql = String.format("select * from chart_%s", chartId);
           List<Map<String, Object>> resultData = chartMapper.queryChartData(querySql);
           System.out.println(resultData);
       }
   }
   ```



## 限流

> [!WARNING]
>
> **问题：**使用系统是需要消耗成本的，用户有可能疯狂刷量

**解决方案：**

1. 控制成本 =》限制用户调用总次数
2. 用户在短时间内疯狂使用，导致服务器资源被占满，其他用户无法使用=》**限流**

> [!TIP]
>
> **思考：**限流阈值多大合适？参考正常用户的使用，比如限制单个用户在每秒只能使用1次。

### 限流的算法

[限流算法面经讲解]: https://juejin.cn/post/6967742960540581918

- 固定窗口限流

  - 单位时间内运行部分操作，首先维护一个计数器，将单位时间段当做一个窗口，计数器记录这个窗口接受请求的次数。

    - 当次数少于限流阈值，就拒绝访问，计数器+1
    - 当次数大于限流阈值，就拒绝访问
    - 当前的时间窗口过去之后，计数器清零

    ```java
        /**
         * 固定窗口时间算法
         * @return
         */
        boolean fixedWindowsTryAcquire() {
            long currentTime = System.currentTimeMillis();  //获取系统当前时间
            if (currentTime - lastRequestTime > windowUnit) {  //检查是否在时间窗口内
                counter = 0;  // 计数器清0
                lastRequestTime = currentTime;  //开启新的时间窗口
            }
            if (counter < threshold) {  // 小于阀值
                counter++;  //计数器加1
                return true;
            }
    
            return false;
        }
    ```

  - > [!WARNING]
    >
    > 这个算法有一个明显的**临界问题**：假设限流阈值为5个请求，单位时间窗口是1s，如果我们在单位时间内的前0.8-1s和1-1.2s，分别并发5个请求。虽然都没有超过阈值，但是如果算0.8-1.2s，则并发数高达10，已经**超过单位时间1s不超过5阈值**的定义了。

- 滑动窗口限流

  - 滑动窗口限流解决固定窗口临界值的问题。它将单位时间周期分为n个小周期，分别记录每个小周期内接口的访问次数，并且根据时间滑动删除过期的小周期。

    ![img](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/b46a1b76b6e04ff7b240291b1fe538cc~tplv-k3u1fbpfcp-zoom-in-crop-mark:1512:0:0:0.awebp)

    假设单位时间还是1s，滑动窗口算法把它划分为5个小周期，也就是滑动窗口（单位时间）被划分为5个小格子。每格表示0.2s，每过0.2s，时间窗口就会往右滑动一格。然后，每个小周期，都有自己独立的计数器，如果请求是0.82s到达的，0.8~1s对应的计数器就会+1。

  - > [!important]
    >
    > 滑动窗口如何解决临界问题：
    >
    > 假设我们1s内的限流阈值还是5个请求，0.8~1.0s内（比如0.9s的时候）来了5个请求，落在黄色格子里。时间过了1.0s这个点之后，又来了5个请求，落在紫色格子里。如果是**固定窗口算法，是不会被限流的**，但是**滑动窗口的话，每过一个小周期，它会右移一个小格**。过了1.0s这个点后，会右移一小格，当前的单位时间段是0.2~1.2s，这个区域的请求已经超过限定的5了，已触发限流了，实际上，紫色格子的请求都被拒绝了。

  - > [!tip]
    >
    > 当滑动窗口的格子周期划分的越多，那么滑动窗口的滚动就越平滑，限流的统计就会越精确。

    ```java
     /**
         * 单位时间划分的小周期（单位时间是1分钟，10s一个小格子窗口，一共6个格子）
         */
        private int SUB_CYCLE = 10;
    
        /**
         * 每分钟限流请求数
         */
        private int thresholdPerMin = 100;
    
        /**
         * 计数器, k-为当前窗口的开始时间值秒，value为当前窗口的计数
         */
        private final TreeMap<Long, Integer> counters = new TreeMap<>();
    
       /**
         * 滑动窗口时间算法实现
         */
        boolean slidingWindowsTryAcquire() {
            long currentWindowTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) / SUB_CYCLE * SUB_CYCLE; //获取当前时间在哪个小周期窗口
            int currentWindowNum = countCurrentWindow(currentWindowTime); //当前窗口总请求数
    
            //超过阀值限流
            if (currentWindowNum >= thresholdPerMin) {
                return false;
            }
    
            //计数器+1
            counters.get(currentWindowTime)++;
            return true;
        }
    
       /**
        * 统计当前窗口的请求数
        */
        private int countCurrentWindow(long currentWindowTime) {
            //计算窗口开始位置
            long startTime = currentWindowTime - SUB_CYCLE* (60s/SUB_CYCLE-1);
            int count = 0;
    
            //遍历存储的计数器
            Iterator<Map.Entry<Long, Integer>> iterator = counters.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Long, Integer> entry = iterator.next();
                // 删除无效过期的子窗口计数器
                if (entry.getKey() < startTime) {
                    iterator.remove();
                } else {
                    //累加当前窗口的所有计数器之和
                    count =count + entry.getValue();
                }
            }
            return count;
        }
    ```

  - > [!CAUTION]
    >
    > 滑动窗口算法虽然解决了固定窗口的临界问题，但是一定到达限流后，请求都会直接暴力被拒绝。这样我们会损失一部分请求，这对于产品来说，并不太友好。

    

- 漏桶限流

  - 以**固定的速率**处理请求（漏水），当请求桶满了后，拒绝请求

    每秒处理10个请求，桶的容量是10，每0.1s固定处理一次请求，如果1s内来了10个请求；都可以处理完，但如果1s内来了11个请求，最后那个请求就会溢出，被拒绝。

  - 优点：能够一定程度上应对流量突刺

    缺点：没有办法迅速处理一批请求，只能一个一个按顺序来处理（固定速率的缺点）

- 令牌桶限流

  面对**突发流量**的时候，可以使用令牌桶算法限流

  **原理：**

  - 有一个令牌管理员，根据限流大小，定速往令牌桶里放令牌

  - 如果令牌数量满了，超过令牌桶容量的限制，就丢弃

  - 系统在接收到一个用户请求时，都会先去令牌桶要一个令牌。如果拿到令牌，那么就处理这个请求的业务逻辑；

    如果拿不到令牌，就直接拒绝这个请求。

  ![img](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/9f8efde12a1a4fb8b902d910a90f104b~tplv-k3u1fbpfcp-zoom-in-crop-mark:1512:0:0:0.awebp)

  ```java
      /**
       * 每秒处理数（放入令牌数量）
       */
      private long putTokenRate;
      
      /**
       * 最后刷新时间
       */
      private long refreshTime;
  
      /**
       * 令牌桶容量
       */
      private long capacity;
      
      /**
       * 当前桶内令牌数
       */
      private long currentToken = 0L;
  
      /**
       * 漏桶算法
       * @return
       */
      boolean tokenBucketTryAcquire() {
  
          long currentTime = System.currentTimeMillis();  //获取系统当前时间
          long generateToken = (currentTime - refreshTime) / 1000 * putTokenRate; //生成的令牌 =(当前时间-上次刷新时间)* 放入令牌的速率
          currentToken = Math.min(capacity, generateToken + currentToken); // 当前令牌数量 = 之前的桶内令牌数量+放入的令牌数量
          refreshTime = currentTime; // 刷新时间
          
          //桶里面还有令牌，请求正常处理
          if (currentToken > 0) {
              currentToken--; //令牌数量-1
              return true;
          }
          
          return false;
      }
  
  ```

- 如果令牌发放的策略正确，这个系统即不会被拖垮，也能提高机器的利用率。Guava的RateLimiiter限流组件，就是基于**令牌桶算法**实现的。

  - 优点：能够并发处理同时的请求，**并发性能会提高**

    需要考虑的问题：存在时间单位选取的问题

### 本地限流

> [!tip]
>
> 自己实现；
>
> 每个服务器单独限流，一般适用于单体限流，就是你的项目只有一个服务器
>
> 举个例子：假设你的系统有三台服务器，每台服务器限制用户每秒只能请求一次。你可以为每台服务器单独设置限流策略，这样每个服务器都能够独立地控制用户的请求频率。但是这种限流方式并不是很可靠，因为你并不知道用户的请求会落在哪台服务器上，它的分布是有一定的偶然性的。即使采用负载均衡技术，让用户请求轮流发送到每台服务器，仍然存在一定的风险。

在java中，有很多第三方库可以用来实现单机限流：

- Guava RateLimiter（**令牌桶算法**）: 谷歌Guava库提供的限流工具，可以对单位时间内的请求数量进行限制。

  ```java
  import com.google.common.util.concurrent.RateLimiter;
  
  public static void main(String[] args) {
      // 每秒限流5个请求
      RateLimiter limiter = RateLimiter.create(5.0);
      while (true) {
          if (limiter.tryAcquire()) {
              // 处理请求
          } else {
              // 超过流量限制，需要做何处理
          }
      }
  }
  ```

### 分布式限流（多机限流）

如果你的项目有多个服务器，比如微服务，那么建议使用分布式限流

1. 把用户的使用频率等数据放到一个集中的存储进行统计；比如Redis，这样无论用户的请求落到了哪台服务器，都以集中存储中的数据为准。（Redisson——是一个操作Redis的工具库）

2. 在网关集中进行限流和统计（比如Sentinel，Spring Cloud Gateway）

   ```java
   import org.redisson.Redisson;
   import org.redisson.api.RSemaphore;
   import org.redisson.api.RedissonClient;
   
   public static void main(String[] args) {
       // 创建RedissonClient
       RedissonClient redisson = Redisson.create();
   
       // 获取限流器
       RSemaphore semaphore = redisson.getSemaphore("mySemaphore");
   
       // 尝试获取许可证
       boolean result = semaphore.tryAcquire();
       if (result) {
           // 处理请求
       } else {
           // 超过流量限制，需要做何处理
       }
   }
   ```

### Redission 限流实现

Redission 内置了一个限流工具类，可以帮助利用 Redis 来存储、统计。

> [!Important]
>
> 可参考星球 - 伙伴匹配系统中Redis使用

**1. 引入Maven依赖**

```xml
<dependency>
  <groupId>org.redisson</groupId>
  <artifactId>redisson</artifactId>
  <version>3.21.3</version>
</dependency> 
```

**2. 在application.yml中配置 redis**

```yml
  # Redis 配置
  # todo 需替换配置，然后取消注释
  redis:
    database: 1
    host: localhost
    port: 6379
    timeout: 5000
#    password: 123456
```

**3. 编写配置类**

创建 RedissonConfig 配置类，用于初始化 RedissonClient 对象单例；

在 config 目录下新建 RedissonConfig.java

```java
package com.yupi.springbootinit.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
// 从application.yml文件中读取前缀为"spring.redis"的配置项
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedissonConfig {

    private Integer database;

    private String host;

    private Integer port;
    // 如果redis默认没有密码，则不用写
    //private String password;

    // spring启动时，会自动创建一个RedissonClient对象
    @Bean
    public RedissonClient getRedissonClient() {
        // 1.创建配置对象
        Config config = new Config();
        // 添加单机Redisson配置
        config.useSingleServer()
        // 设置数据库
        .setDatabase(database)
        // 设置redis的地址
        .setAddress("redis://" + host + ":" + port);
        // 设置redis的密码(redis有密码才设置)
        //                .setPassword(password);

        // 2.创建Redisson实例
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }
}
```

**4. 编写Manager**

- 编写 RedisLimiterManager：什么是 Manager？

  专门提供 RedisLimiter 限流基础服务的（提供了通用的能力，可以作为 template 放到任何一个项目中）

  ```java
  package com.yupi.springbootinit.manager;
  
  
  import com.yupi.springbootinit.common.ErrorCode;
  import com.yupi.springbootinit.exception.BusinessException;
  import org.redisson.api.RRateLimiter;
  import org.redisson.api.RateIntervalUnit;
  import org.redisson.api.RateType;
  import org.redisson.api.RedissonClient;
  import org.springframework.stereotype.Service;
  
  import javax.annotation.Resource;
  
  /**
   * 专门提供 RedisLimiter 限流基础服务的（提供了通用的能力，放其他项目都能用）
   */
  @Service
  public class RedisLimiterManager {
  
      @Resource
      private RedissonClient redissonClient;
  
      /**
       * 限流操作
       * @param key 区分不同的限流器，比如不同的用户 id 应该分别统计
       */
      public void doRateLimit(String key){
          // 创建一个名为key的限流器
          RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
          /**
           * RateType：限流模式，RateType.OVERALL 表示所有实例共享限流器，RateType.PER_CLIENT 表示每个客户端独立限流。
           * rate：时间窗口内允许的令牌数量，一个请求对应一个令牌。
           * rateInterval：时间窗口的间隔。
           * RateIntervalUnit：时间窗口的单位（如秒、毫秒等）
           */
          rateLimiter.trySetRate(RateType.OVERALL,2,1, RateIntervalUnit.SECONDS);
          /**
           * 每当1个操作来了后，请求1个令牌，也可以设置为一个操作请求2个令牌，这样可以区分vip用户和普通用户
           * vip用户可以设置为一次请求需要1个令牌，则一个时间窗口内可以最多进行2次请求
           * 普通用户可以设置为一次请求需要2个令牌，则一个时间窗口内可以最多进行1次请求
           */
          boolean canOp = rateLimiter.tryAcquire(1);
          // 若没有令牌，则抛出异常
          if(!canOp){
              throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
          }
      }
  }
  ```

**5. 应用到实际业务中**

- 给每一个用户分配一个限流器

  在 genChartByAI 接口做一个限流器判断

  ```java
  // 引用
  @Resource
  private RedisLimiterManager redisLimiterManager;
  
  
  // 限流判断，每个用户一个限流器
  redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());
  ```

  由于限流操作可能会抛出异常，因此当请求到达时，如果无法获取到令牌，则将抛出异常并终止请求；反之，如果成功获取到令牌，则请求可以正常继续执行，此时不需要进行其他任何操作。

- 可以在抛出异常时提示用户，“你的权限不足”之类的

**限流粒度**

1. 针对某个方法限流：即单位时间内最多运行XX个操作使用这个方法
2. 针对某个用户限流：比如单个用户单位时间内最多执行XX次操作
3. 针对某个用户的x方法限流：比如单个用户单位时间内最多执行XX次这个方法

### Tips 限流实现Vip用户的区分

```java
/**
 * 专门提供 RedisLimiter 限流基础服务的（提供了通用的能力，放其他项目都能用）
 */
@Service
public class RedisLimiterManager {

    @Resource
    private RedissonClient redissonClient;

    /**
     * 限流操作
     * @param key 区分不同的限流器，比如不同的用户 id 应该分别统计
     */
    public void doRateLimit(String key){
        // 创建一个名为key的限流器
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        /**
         * RateType：限流模式，RateType.OVERALL 表示所有实例共享限流器，RateType.PER_CLIENT 表示每个客户端独立限流。
         * rate：时间窗口内允许的令牌数量，一个请求对应一个令牌。
         * rateInterval：时间窗口的间隔。
         * RateIntervalUnit：时间窗口的单位（如秒、毫秒等）
         */
        rateLimiter.trySetRate(RateType.OVERALL,2,1, RateIntervalUnit.SECONDS);
        /**
         * 每当1个操作来了后，请求1个令牌，也可以设置为一个操作请求2个令牌，这样可以区分vip用户和普通用户
         * vip用户可以设置为一次请求需要1个令牌，则一个时间窗口内可以最多进行2次请求
         * 普通用户可以设置为一次请求需要2个令牌，则一个时间窗口内可以最多进行1次请求
         */
        boolean canOp = rateLimiter.tryAcquire(1);
        // 若没有令牌，则抛出异常
        if(!canOp){
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }
    }
}
```



---

## Redis入门

> NoSql数据库

Key-value 存储系统（区别于Mysql，他存储的是键值对）

**Java中的实现方式**

- **Spring Data Redis**

  同样的数据访问框架，定义了一组增删改查的接口

- jedis

- Redisson

**待补充。。。**



---

## 系统优化问题分析

**问题场景：调用的服务处理能力有限，或者接口处理时间较长，就应该考虑异步化了**

1. 用户等待时间长（因为要等AI生成）
2. 业务服务器可能会有很多请求在处理，导致系统资源紧张，严重时导致服务器宕机或者无法处理新的请求
3. 调用第三方服务（AI能力）的处理能力是有限的，比如每3秒智能处理1个请求，会导致AI处理不过来，严重时AI可能会对咱们的后台系统拒绝服务。

---

## 异步化

**同步：**一件事情做完，再做另外一件事情（烧水后才能处理工作）

**异步：**在处理一件事情的同时，可以处理另一件事情。当第一件事完成时，会收到一个通知告诉你这件事已经完成，这样就可以进行后续的处理（烧水时，可以同时处理其他准备工作。水壶有一个蜂鸣器，在处理其他工作的同时，水烧好后人能听到声音，知道水烧好了）

回归到本系统，就是在ai生成图表的时候，用户可以进行其他操作，比如查看自己的历史数据，修改个人信息等。等图表生成完成，可以有一个消息通知提示用户，图表生成成功。

### 标准异步化业务流程

在用户需要进行长时间的操作时，点击提交后不需要在页面空等。而是先保存至数据库。以往我们会等图表完全生成后再保存，但现在，任务一提交，我们就立即存储，避免让用户在界面上等待。

接着，需要**将用户的操作或任务添加到任务队列**中，让程序或线程执行。想象一下，将用户的操作加入任务队列，这个队列就像一个备忘录。比如我是公司唯一的员工，正在进行一个项目，当有用户请求修复bug时，我无法立即处理，但可以记下这个修复bug的任务，待完成项目后再处理。这个队列就像我的备忘录。

由于程序的处理能力或线程数有限，我们可以先把待处理的任务放入队列中等待。当我们有空的时候，再按顺序执行，而不是直接拒绝。因此，如果任务队列中有空位，我们可以接受新任务；如果有空闲的线程或员工，我们可以立即开始这个任务；如果所有线程都在忙碌，那么我们可以把任务放入等待队列。**但是，如果所有线程都在忙，且任务队列已满，那么怎么办？**

一种做法是直接拒绝任务，更好的方式是记录下这个任务，待有空时再处理。无论任务提交成功与否，我们都应该将其保存到数据库中以备查阅。这样，当我们在后期检查时，可以看到哪些任务由于程序处理能力不足而未得到处理。即使任务队列满了，我们也可以通过查阅数据库记录，找出提交失败的任务，并在程序空闲时取出这些任务，放入任务队列中执行。

当用户需要执行新任务时，即使任务提交失败，或者消息队列满了，也要将其记录下来。**建议将这个任务保存到数据库中记录，或者至少打一个日志，以应对网络或电力的突发情况。**

在第三步中，我们的程序（线程）会按照任务队列的顺序逐一执行任务，这就像员工按照备忘录一项接一项地完成任务。任务完成后，我们会更新任务状态，将相关任务记录在数据库中标记为已完成。接下来需要考虑的问题是如何让用户知道他们的任务何时完成。在用户提交任务后，我们应该提供一个查询任务状态的地方，而不是让他们无尽地等待。

通过这一系列流程，用户的体验会比直接等待任务完成更好。尤其是在需要进行复杂分析的情况下，用户不太可能在界面上等到那么久。这时，我们可以采取**异步执行**，让用户先去做其他事情，可以继续提交新任务，也可以实时查看任务状态。这样的体验更好，远优于等待长时间后任务失败。

---

> [!caution]
>
> 并非所有的操作都需要异步化。只有在任务执行时间较长的场景下，才考虑采用异步化方式。因为多线程和异步处理会增加代码的复杂度，并可能带来更多的问题。如果同步方式能解决问题，那么就无需使用异步。

异步处理是一个复杂的过程。在异步之中，开发者可能无法清晰地知道程序执行到了哪一步。对于复杂的任务，我们需要在每一个小任务完成时记录下任务的执行状态或进度，这就像我们下载文件看进度条一样。**所以对于大型、复杂的任务，为了提供更好的用户体验，我们应该提供进度条，**让用户在查询状态时能看到任务执行到了哪一步。这时任何异步操作的重要环节，也是优化业务流程的方法。

#### 标准异步化步骤总结

1. 当用户要进行耗时很长的操作时，点击提交后，不需要在界面空等，而是应该把这个任务保存到数据库中记录下来。
2. 用户执行新任务时
   1. 任务提交成功
      1. 若程序存在空闲线程，可以立即执行此任务
      2. 若所有线程均繁忙，任务将入队列等待处理
   2. 任务提交失败：比如所有线程都在忙碌且**任务队列满了**
      1. 选择拒绝此任务，不再执行
      2. 通过查阅数据库记录，发现提交失败的任务，并在程序空闲时将这些任务取出执行。
3. 程序（线程）从任务队列中取出任务依次执行，每完成一项任务，就更新任务状态。
4. 用户可以查询任务的执行状态，或者在任务执行成功或失败时接收通知（例如：发邮件、系统消息提示或短信）
5. 对于复杂且包含多个环节的任务，在每个小任务完成时，要在程序（数据库中）记录任务的执行状态（进度）

### 系统业务流程总结

1. 用户点击智能分析页的提交按钮，**先把图表立刻保存到数据库中**（就是在执行ai操作之前，先把用户提交的数据保存到数据库中）
2. 用户可以在图表管理页面查看所有图表（已生成的、生成中的、生成失败）的信息和状态
3. 用户可以修改生成失败的图表信息，点击重新生成，以尝试再次创建图表。

**系统新架构图**

![image-20250216211109579](../../../Library/Application Support/typora-user-images/image-20250216211109579.png)

**问题：**

1. 任务队列的最大容量应该设置为多少？
2. 程序怎么从任务队列中取出任务去执行？这个任务队列的流程怎么实现？怎么保证程序最多同时执行多少个任务？

## 线程池

线程池可以解决上述的问题

- 要根据需求进行测试来确定线程池的参数

- 需要结合实际情况，考虑系统最脆弱的环境，或者找出系统的瓶颈在哪里。

  假设最多运行4个线程同时运行，最多运行20个任务排队。

  如果同时提交了50个任务，那么有4个任务会被执行，20个任务排队，剩下的26个任务就会被丢弃。因此，我们的线程池参数需要根据这些条件来设定。

### 线程池总结

**为什么需要线程池**

1. 线程的管理比较复杂（比如什么时候新增线程、什么时候减少空闲线程）
2. 任务存取比较复杂（什么时候接收任务、什么时候拒绝任务、怎么保证大家不抢到同一个任务）

**线程池的作用：**帮助你轻松管理线程、协调任务的执行过程。

**扩充：**可以向线程池表达你的需求，比如最多只允许四个人同时执行任务。线程池就能自动为你进行管理。在任务紧急时，它会帮你将任务放入队列。而在任务不紧急或者还有线程空闲时，它会直接将任务交给空闲的线程，而不是放入队列。

### 线程池参数

> [!important]
>
> 经典面试题，讲线程有什么参数，平时是怎么去设置线程池参数的

```java
public ThreadPoolExecutor(int corePoolSize,
                          int maximumPoolSize,
                          long keepAliveTime,
                          TimeUnit unit,
                          BlockingQueue<Runnable> workQueue,
                          ThreadFactory threadFactory,
                          RejectedExecutionHandler handler) {

```

- 第一个参数 corePoolSize (核心线程数)。这些线程就好比是公司的正式员工，他们在正常情况下都是随时待命处理任
  务的。如何去设定这个参数呢?比如，如果我们的 A1服务只允许四个任务同时进行，那么我们的核心线程数应该就被设置为四。
- 第二个参数 maximumPoolSize (最大线程数)。在极限情况下我们的系统或线程池能有多少个线程在工作。**就算任务再多，你最多也只能雇佣这么多的人**，因为你需要考虑成本和资源的问题。**假设 A1服务最多只允许四个任务同时执行，那么最大线程数应当设置为四。**
- 第三个参数 keepAliveTime (空闲线程存活时间)。这个参数决定了当任务少的时候，临时雇佣的线程会等待多久才会被剔除。这个参数的设定是为了释放无用的线程资源。你可以理解为，多久之后会"解雇"没有任务做的临时工。
- 第四个参数 TimeUnit (空闲线程存活时间的单位)。将 keepAliveTime 和 TimeUnit 组合在一起，就能指定一个具体的时间，比如说分钟、秒等等。
- 第五个参数 workQueue (工作队列)，也就是任务队列。这个队列存储所有等待执行的任务。也可以叫它阻塞队列,因为线程需要按顺序从队列中取出任务来执行。**这个队列的长度一定要设定**，因为无限长度的队列会消耗大量的系统资源。
- 第六个参数 threadFactory(线程工厂)。它负责控制每个线程的生成，就像一个管理员，负责招聘、管理员工，比如设定员工的名字、工资，或者其他属性。
- 第七个参数 **ReiectedExecutionHandler(拒绝策略)**。当任务队列已满的时候，我们应该怎么处理新来的任务?是抛出异常，还是使用其他策略?比如说，我们可以设定任务的优先级，会员的任务优先级更高。如果你的公司或者产品中有会员业务，或者有一些重要的业务需要保证不被打扰，你可以考虑定义两个线程池或者两个任务队列，一个用于处理VIP任务，一个用于处理普通任务，保证他们不互相干扰，也就是**资源隔离策略。(可以写到简历上的点)**
  这就是线程池的各个参数的作用，理解了这些，你就可以更好地根据你的业务需求来设定和优化你的线程池了,

## 线程池的工作机制

刚开始，没有任何的线程和任务：

1. 当有新任务进来，发现当前员工数量还未达到设定的正式员工数（corePoolSize = 2），则会直接安排一位正式员工来处理
2. 又来了一个新任务，发现当前员工数量还没达到设定的正式员工数（corePoolSize = 2），则会再次安排一位正式员工来处理
3. 这时，来了第三个任务，但是正式员工数已经达到上限（当前线程数 = corePoolSize = 2），这个新任务将被放到等待队列中（最大长度 workQueue.size = 2），而不是直接增聘新员工
4. 又来了一个新任务，但是我们的任务队列已经满了(当前线程数>corePoolSize=2，已有任务数=最大长度workQueue.size = 2)，我们将增设新线程(最大线程数 maximumPoolSize=4)来处理任务，而不是选择丢弃这个任务
5. 当达到七个任务时，由于我们的任务队列已经满了、临时工也招满了(当前线程数=maximumPoolSize4，已有任务数=最大长度 workQueue.size=2)，此时我们会采用 RejectedExecutionHandler(拒绝策略来处理多余的任务:
6. 如果当前线程数超过 corePoolSize (正式员工数)，并且这些额外的线程没有新的任务可执行，那么在 keepAliveTime 时间达到后，这些额外的线程将会被释放。

### 线程池参数设置讲解

- 首先，corePoolSize 参数非常关键，它代表了在正常情况下需要的线程数量。你可以根据希望的系统运行状况以及同时执行的任务数设定这个值。
- 接着是 maximumPoolSize，这个参数的设定应当与我们的下游系统的瓶颈相关联。比如，如果我们的 A1 系统一次只允许两个任务同时执行，那么 maximumPoolSize 就应设为两个，这就是其上限，不宜过大。所以，这个参数值的设定就应当对应于极限情况。 让我们回到我们的业务场景，我们的 A系统最多允许4个任务同时执行，那么我们应如何设定这些参数呢?对于核心线程数(corePoolSize)，你可以设定为四，这是在正常运行情况下的需求。然后，maxmumPoolSize 就应设定为极限条件，也就是小于等于 4。
- 至于 keepAliveTime 空闲存活时间，并不需要过于纠结，这个问题相对较小。你可以设定为几秒钟，虽然这可能稍微有点短。你可能需要根据任务以及人员的变动频率进行设定，但无需过于纠结，通常设定为秒级或分钟级别就可以了。
- 再看 workQueue 工作队列的长度，建议你结合系统的瓶颈进行设定。在我们的场景中，可以设定为 20。如果下游系统最多允许一小时的任务排队，那么你这边就可以设置 20 个任务排队，而核心线程数则设定为 4。
- threadFactory 线程工厂这里就不细说了，应根据具体情况设定。至于 RejectedExecutionHandler 拒绝策略，我们可以直接选择丢弃、抛出异常，然后交由数据库处理，或者标记任务状态为已拒绝，表示任务已满，无法再接受新的任务。

> [!NOTE]
>
> 一般情况下，任务分为 IO 密集型和计算密集型两种
>
> - 计算密集型：**吃 CPU**，比如音视频处理、图像处理、数学计算等，一般是设置 corePoolSize 为 CPU 的核数 + 1（空余线程），可以让每个线程都能利用好 CPU 的每个核，而且线程之间不用频繁切换（减少打架、减少开销）
> - IO 密集型：**吃带宽/内存/硬盘的读写资源**，corePoolSize 可以设置大一点，一般经验是2n左右，但是建议以 IO 的能力为主。
>
> 线程池的设计主要分为 IO 密集型和计算密集型。 在面试中，如果面试官问你关于线程池的设置，首先你雲要明确设置的依据应该是具体的业务场景。通常，我们可以将任务分为 10 密集型和 CPU 密集型，也称为计算密集型。对于计算密集型的任务，它会大量消耗 CPU 资源进行计算，例如音视频处理、图像处理、程序计算和数学计算等。**要最大程度上利用 CPU，避免多个线程间的冲突，一般将核心线程数设置为 CPU 的核数加一。这个"加一"可以理解为预留一个额外的线程，或者说一个备用线程，来处理其他任务。这样做可以充分利用每个 CPU 核心，减少线程间的频繁切换，降低开销。**在这种情况下，对 maximumPoolSize 的设定没有严格的规则，一般可以设为核心线程数的两倍或三倍。 
>
> 而对于 IO 密集型的任务，它主要消耗的是带宽或内存硬盘的读写资源，对 CPU 的利用率不高。比如说，查询数据库或等待网络消息传输，可能需要花费几秒钟，**而在这期间 CPU 实际上是空闲的。在这种情况下，可以适当增大 corePoolSize 的值，因为 CPU 本来就是空闲的。**比如说，如果数据库能同时支持 20 个线程查询，那么corePoolSize 就可以设置得相对较大，以提高查询效率。虽然有一些经验值，比如 2N+1，不太推崇这种经验值，建议根据 IO 的能力来设定。



### 线程池的实现

> 在大厂面试时，有可能会让你自行实现线程池。这时，你可以借助之前提到的几种场景，例如何时增加线程，何时减少线程，来逐步解答这个问题。实际上这是一项繁琐的任务。比如，有一种情况，如果大家执行速度都很快，可能会抢到同一个任务，如何协调各线程不去抢相同的任务就成了问题，这就涉及到线程的协调问题，在 Linux 中，有一种称为任务窃取的概念。比如，如果小鱼的工作效率非常高，而小李的工作效率较低，我们可以将原本分配给小李的任务，转交给小鱼去做。这就是任务窃取的概念。这个问题非常复杂，涉及到的内容很深。任务是否需要锁，取决于你使用的数据结构类型，例如你是否使用了阻塞队列等来实现任务队列，这些策略都相当复杂。

**针对当前系统设置参数**

现有条件:比如 A 生成能力的并发是只允许4个任务同时去执行，AI能力允许 20 个任务排队。

- corePoolSize(核心线程数 =>正式员工数):正常情况下,可以设置为 2-4maximumPoolSize:设置为极限情况，设置为<=4
- keepAliveTime(空闲线程存活时间):一般设置为秒级或者分钟级
- TimeUnit unit(空闲线程存活时间的单位):分钟、秒
- workQueue(工作队列):结合实际请况去设置，可以设置为 20
- threadFactory(线程工厂):控制每个线程的生成、线程的属性(比如线程名）
- RejectedExecutionHandler(拒绝策略):抛异常，标记数据库的任务状态为"任务满了已拒绝'

- 不用自己写，如果是在Spring中，可以用 ThreadPoolTaskExecutor 配合 @Async 注解来实现。（不太建议）

  > ps.虽然 Spring 框架提供了线程池的实现，但并不特别推荐使用。因为 Spring 毕竟是一个框架，它进行了一定程度的封装，可能隐藏了一些细节。更推荐大家直接使用 java 并发包中的线程池，这也是🐟之前公司常用的做法,请注意，这并不是绝对不使用 Spring 的线程池，只是个人的建议，对其使用有一定的保留意见。

- 如果是在 Java 中，可以使用 JUC 并发编程包中的 ThreadPoolExecutor 来实现非常灵活地自定义线程池。

  > ps.建议学完 SpringBoot 并能够实现一个项目，以及学完 Redis 之后，再系统学习 Java 并发编程(JUC)。这样可以避免过早的压力和困扰，在具备一定实践基础的情况下，更好地理解并发编程的概念和应用

  ```java
  package com.yupi.springbootinit.config;
  
  
  import org.jetbrains.annotations.NotNull;
  import org.springframework.context.annotation.Bean;
  import org.springframework.context.annotation.Configuration;
  
  import java.util.concurrent.ArrayBlockingQueue;
  import java.util.concurrent.ThreadFactory;
  import java.util.concurrent.ThreadPoolExecutor;
  import java.util.concurrent.TimeUnit;
  
  @Configuration
  public class ThreadPoolExecutorConfig {
      @Bean
      public ThreadPoolExecutor threadPoolExecutor() {
          // 创建一个线程工厂
          ThreadFactory threadFactory = new ThreadFactory() {
              // 初始化线程数为1
              private int count = 1;
              @Override
              // 每当线程池需要创建新线程时，就会调用newThread方法
              // @NotNull Runnable r 表示方法参数，r就是要传入线程执行的任务
              // 如果这个方法被调用的时候传递了一个null参数，就会报错
              public Thread newThread(@NotNull Runnable r) {
                  Thread thread = new Thread(r);
                  // 给新线程设置一个名称，名称中包含线程数的当前值
                  thread.setName("线程"+count);
                  count++;
                  return thread;
              }
          };
          // 创建一个新的线程池，线程池核心大小为2；最大线程数为4；
          // 非核心线程空闲时间为100s；任务队列为阻塞队列，长度为4；使用自定义的线程工厂创建线程
          ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(2,4,100, TimeUnit.SECONDS,
                  new ArrayBlockingQueue<>(4),threadFactory);
          return threadPoolExecutor;
      }
  }
  ```

- 测试队列，可以在Swagger的localhost:port/api/doc.html，中进行接口调试

  ```java
  package com.yupi.springbootinit.controller;
  
  import cn.hutool.json.JSONUtil;
  import lombok.extern.slf4j.Slf4j;
  import org.springframework.web.bind.annotation.*;
  
  import javax.annotation.Resource;
  import java.util.HashMap;
  import java.util.Map;
  import java.util.concurrent.CompletableFuture;
  import java.util.concurrent.ThreadPoolExecutor;
  
  
  /**
   * 队列测试
   *
   * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
   * @from <a href="https://yupi.icu">编程导航知识星球</a>
   */
  @RestController
  @RequestMapping("/queue")
  @Slf4j
  public class QueueController {
  
      @Resource
      // 注入一个线程池的实例
      private ThreadPoolExecutor threadPoolExecutor;
  
      @GetMapping("/add")
      public void add(String name){
          // 使用CompletableFuture运行一个异步任务
          CompletableFuture.runAsync(() -> {
              log.info("任务执行中："+name+",执行人："+Thread.currentThread().getName());
              try {
                  Thread.sleep(600000);
              } catch (InterruptedException e) {
                  e.printStackTrace();
              }
          }, threadPoolExecutor);
      }
      @GetMapping("/get")
      /**
       * 返回线程池的状态信息
       */
      public String get(){
          Map<String,Object> map = new HashMap<>();
          // 线程池的队列长度
          int size = threadPoolExecutor.getQueue().size();
          map.put("队列长度",size);
          // 获取线程池已接收的任务总数
          long taskCount = threadPoolExecutor.getTaskCount();
          map.put("任务总数",taskCount);
          long completedTaskCount = threadPoolExecutor.getCompletedTaskCount();
          map.put("已完成任务数：",completedTaskCount);
          int activeCount = threadPoolExecutor.getActiveCount();
          map.put("正在工作的线程数",activeCount);
          return JSONUtil.toJsonStr(map);
      }
  }
  ```

---



## 前后端异步化改造

### 异步化实现工作流程

1. **给 chart 表新增任务状态字段**（比如排队中、执行中、已成功、失败），任务执行信息字段（用于记录任务执行中、或者失败的一些信息）

   ```sql
   -- 任务状态字段(排队中wait、执行中running、已完成succeed、失败failed)
   status       varchar(128) not null default 'wait' comment 'wait,running,succeed,failed',
   -- 任务执行信息字段
   execMessage  text   null comment '执行信息',
   ```

2. 用户点击智能分析页的提交按钮时，先把图表立刻保存到数据库中，然后提交任务

3. 任务：先修改图表任务状态为 **“执行中”**。等执行成功后，修改为 “已完成”，保存执行结果；执行失败后，状态修改为 “失败”，记录任务失败信息。

4. 用户可以在图表管理页面查看所有图表（已生成的、生成中的、生成失败）的信息和状态 --》（优化点）

5. 用户可以修改生成失败的图表信息，点击重新生成 --》（优化点）

