package RabbitmqTest.Publish_Subcribe.direct;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

public class DirectProducer {
    private static final String EXCHANGE_NAME = "direct-ecchange";

    public static void main(String[] args) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try(Connection connection = factory.newConnection();
            Channel channel = connection.createChannel()){
            channel.exchangeDeclare(EXCHANGE_NAME,"direct");

            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNext()){
                String userInput = scanner.nextLine();
                String[] strings = userInput.split(" ");
                if (strings.length < 1){
                    continue;
                }
                String message = strings[0];
                String routingKey = strings[1];

                // 发布消息到直连交换机
                // 使用通道的basicPublish方法将消息发布到交换机
                // EXCHANGE_NAME 表示要发布消息的交换机的名称
                // routingKey表示消息的路由键，用于确定消息被路由到哪个队列
                // null 表示不使用额外的消息属性
                // message.getBytes("UTF-8")将消息内容转换为UTF-8编码的字节数组
                channel.basicPublish(EXCHANGE_NAME,routingKey,null,message.getBytes("UTF-8"));
                System.out.println(" [x] Sent '" + message + " with routing:"+routingKey+"'");

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
