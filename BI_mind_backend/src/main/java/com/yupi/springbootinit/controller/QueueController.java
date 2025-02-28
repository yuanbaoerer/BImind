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
 * 仅作为队列测试用
 *
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
