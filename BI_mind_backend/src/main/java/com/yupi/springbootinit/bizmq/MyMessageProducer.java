package com.yupi.springbootinit.bizmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class MyMessageProducer {
    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送消息的方法
     * @param exchange 交换机名称，指定消息要发送到哪个交换机
     * @param routingKey 路由键，指定消息要根据什么规则路由到相应的队列
     * @param message 消息内容，要发送的具体内容
     */
    public void sendMessage(String exchange, String routingKey, String message){
        rabbitTemplate.convertAndSend(exchange,routingKey,message);
    }
}
