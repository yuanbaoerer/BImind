Tips

- 前端和后端链接的网络服务代码，可以使用前端的openapi插件一键生成，会在Service/controller中生成



## 不解之处

1. @Component：标记组件的类
   1. spring中的组件是什么？
2. @Repository：标记为数据访问层的类
   1. 数据访问层是哪一部分？



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



