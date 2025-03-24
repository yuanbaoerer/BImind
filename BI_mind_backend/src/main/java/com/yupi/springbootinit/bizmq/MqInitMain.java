package com.yupi.springbootinit.bizmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class MqInitMain {
    public static void main(String[] args) {
        try{
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            String EXCHANGE_NAME = "code_exchange";
            channel.exchangeDeclare(EXCHANGE_NAME,"direct");

            String queueName = "code_queue";
            String routingKey = "my_routingKey";
            channel.queueDeclare(queueName,true,false,false,null);
            channel.queueBind(queueName,EXCHANGE_NAME,routingKey);
        }catch (Exception e){
            // 异常处理
        }

    }
}
