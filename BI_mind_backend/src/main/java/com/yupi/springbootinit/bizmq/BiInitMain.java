package com.yupi.springbootinit.bizmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * 创建需要的exchange和queue
 */
public class BiInitMain {
    public static void main(String[] args) {
        try{
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            String EXCHANGE_NAME = BiMqConstant.BI_EXCHANGE_NAME;
            channel.exchangeDeclare(EXCHANGE_NAME,"direct");

            String queueName = BiMqConstant.BI_QUEUE_NAME;
            String routingKey = BiMqConstant.BI_ROUTING_KEY;
            channel.queueDeclare(queueName,true,false,false,null);
            channel.queueBind(queueName,EXCHANGE_NAME,routingKey);
        }catch (Exception e){
            // 异常处理
        }

    }
}
