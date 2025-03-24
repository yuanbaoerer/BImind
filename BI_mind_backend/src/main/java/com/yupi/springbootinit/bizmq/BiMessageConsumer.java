package com.yupi.springbootinit.bizmq;


import com.rabbitmq.client.Channel;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.common.Status;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.manager.AIManager;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.service.ChartService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class BiMessageConsumer {
    @Resource
    private AIManager aiManager;

    @Resource
    private ChartService chartService;

    /**
     * 接受消息的方法
     * @param message 接收到的消息内容，是一个字符串类型
     * @param channel 消息所在的通道，可以通过该通道与 RabbitMQ 进行交互，例如手动确认消息、拒绝消息等
     * @param deliveryTag 消息的投递标签，用于唯一标识一条消息
     */
    // 使用@SneakyThrows注解简化异常处理
    @SneakyThrows
    // 使用@RabbitListener注解指定要监听的队列名称为"code_queue"，并设置消息的确认机制为手动确认
    @RabbitListener(queues = {BiMqConstant.BI_QUEUE_NAME},ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag){
        // 使用日志记录器打印接收到的消息内容
        log.info("receiveMessage message = {}",message);
        if (StringUtils.isBlank(message)){
            //如果更新失败，拒绝当前信息，让消息重新进入队列
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"消息为空");
        }
        // 传过来的message就是chartId
        long chartId = Long.parseLong(message);
        Chart chart = chartService.getById(chartId);
        if (chart == null){
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"图表为空");
        }
        Chart updateChart = new Chart();
        updateChart.setId(chart.getId());
        updateChart.setStatus(Status.RUNNING.getStatus());
        boolean b = chartService.updateById(updateChart);
        if (!b){
            // 如果更新图表running状态失败，拒绝消息并处理图表更新错误
            channel.basicNack(deliveryTag,false,false);
            handleChartUpdateError(chart.getId(),"更新图表running状态失败");
            return;
        }

        String userInput = buildUserInput(chart);

        String result = aiManager.sendMsgToXingHuo(true,userInput.toString());
// 对返回结果做拆分，按照5个中括号进行拆分
        String[] splits = result.split("【【【【【");
        // 拆分后进行验证
        if (splits.length < 3){
            channel.basicNack(deliveryTag,false,false);
            handleChartUpdateError(chart.getId(),"AI 生成错误");
            return;
        }

        String genChart = splits[1].trim();
        int indexOne = genChart.indexOf("{");
        int indexTwo = genChart.lastIndexOf("}");
        genChart = genChart.substring(indexOne, indexTwo + 1);
        String genResult = splits[2].trim();

        // 调用AI得到结果之后，再更新一次数据库
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chart.getId());
        updateChartResult.setGenChart(genChart);
        updateChartResult.setGenResult(genResult);
        updateChartResult.setStatus(Status.SUCCEED.getStatus());
        boolean updateResult = chartService.updateById(updateChartResult);
        if (!updateResult){
            channel.basicNack(deliveryTag,false,false);
            handleChartUpdateError(chart.getId(),"更新图表succeed状态失败");
        }
        // 投递标签是一个数字标签，它在消息消费者接收到消息后用于向RabbitMQ确认消息的处理状态，通过将投递标签传递给channel.basicAck(deliveryTag,false)方法，可以告知RabbitMQ该消息已经成功处理，可以进行确认和从队列中删除。
        // 手动确认消息的接收，向RabbitMQ发送确认消息
        channel.basicAck(deliveryTag,false);
    }
    /**
     * 上面的很多接口用到异常，这里自定义一个工具类
     * @param chartId
     * @param execMessage
     */
    private void handleChartUpdateError(long chartId, String execMessage){
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setStatus(Status.FAILED.getStatus());
        updateChartResult.setExecMessage(execMessage);
        boolean updateResult = chartService.updateById(updateChartResult);
        if (!updateResult){
            log.error("更新图表 failed 状态失败" + chartId + ","+execMessage);
        }
    }

    /**
     * 构建用户输入
     * @param chart 图表对象
     * @return 用户输入字符串
     */
    private String buildUserInput(Chart chart) {
        // 获取图表的目标、类型和数据
        String goal = chart.getGoal();
        String chartType = chart.getChartType();
        String csvData = chart.getChartData();

        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");

        // 拼接分析目标
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        userInput.append(csvData).append("\n");
        // 将StringBuilder转换为String并返回
        return userInput.toString();
    }

}
