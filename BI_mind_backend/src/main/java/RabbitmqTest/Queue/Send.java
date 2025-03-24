package RabbitmqTest.Queue;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Send {
    private final static String QUEUE_NAME = "hello";

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            String message = "Hello World!";
            LocalDateTime currentDateTime = LocalDateTime.now();
            // 创建 DateTimeFormatter 对象，指定日期时间格式
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            // 格式化日期时间
            String formattedDateTime = currentDateTime.format(formatter);

            // 拼接消息和格式化后的日期时间
            String combinedMessage = message + " - " + formattedDateTime;

            channel.basicPublish("", QUEUE_NAME, null, combinedMessage.getBytes());

            System.out.println(" [x] Sent '" + combinedMessage + "'");
        }
    }
}