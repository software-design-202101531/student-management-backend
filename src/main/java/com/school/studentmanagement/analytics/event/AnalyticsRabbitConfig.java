package com.school.studentmanagement.analytics.event;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 분석 증분 적재용 RabbitMQ 토폴로지.
 * 운영 도메인 변경 이벤트를 Topic Exchange(analytics.events)로 발행하고,
 * 분석 컨슈머 큐가 모든 라우팅 키(#)를 구독한다. 처리 실패 메시지는 DLQ로 격리.
 */
@Configuration
public class AnalyticsRabbitConfig {

    public static final String EXCHANGE = "analytics.events";
    public static final String QUEUE = "analytics.etl.queue";
    public static final String DLX = "analytics.events.dlx";
    public static final String DLQ = "analytics.etl.queue.dlq";

    // 라우팅 키 (운영 도메인 변경 종류)
    public static final String RK_GRADE_SAVED = "grade.saved";
    public static final String RK_SUBMISSION_CREATED = "submission.created";
    public static final String RK_ATTENDANCE_RECORDED = "attendance.recorded";
    public static final String RK_FEEDBACK_PUBLISHED = "feedback.published";

    @Bean
    public TopicExchange analyticsExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public DirectExchange analyticsDlx() {
        return new DirectExchange(DLX);
    }

    @Bean
    public Queue analyticsQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(DLQ)
                .build();
    }

    @Bean
    public Queue analyticsDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding analyticsBinding() {
        return BindingBuilder.bind(analyticsQueue()).to(analyticsExchange()).with("#");
    }

    @Bean
    public Binding analyticsDlqBinding() {
        return BindingBuilder.bind(analyticsDlq()).to(analyticsDlx()).with(DLQ);
    }

    // 메시지 JSON 직렬화 — RabbitTemplate/리스너 컨테이너가 이 빈을 사용
    @Bean
    public MessageConverter analyticsJsonConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
