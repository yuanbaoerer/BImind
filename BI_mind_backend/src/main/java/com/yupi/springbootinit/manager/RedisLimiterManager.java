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
         *
         * rate * reteInterval 限定1s最多2次请求
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
