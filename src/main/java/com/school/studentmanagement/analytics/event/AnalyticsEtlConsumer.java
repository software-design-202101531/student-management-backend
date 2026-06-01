package com.school.studentmanagement.analytics.event;

import com.school.studentmanagement.analytics.etl.AnalyticsEtlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * 분석 증분 적재 컨슈머. 라우팅 키별로 영향받은 그레인만 멱등 재집계(JdbcTemplate, analytics 스키마).
 * 실패 시 메시지는 DLQ로(default-requeue-rejected=false). 야간 배치가 전량 보정(람다식).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsEtlConsumer {

    private final AnalyticsEtlService etlService;

    @RabbitListener(queues = AnalyticsRabbitConfig.QUEUE)
    public void onEvent(AnalyticsEventMessage message,
                        @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        switch (routingKey) {
            case AnalyticsRabbitConfig.RK_GRADE_SAVED -> {
                etlService.refreshSubjectSummary(message.studentId(), message.subjectId());
                etlService.refreshClassroomDistribution(message.studentId(), message.subjectId());
            }
            case AnalyticsRabbitConfig.RK_SUBMISSION_CREATED ->
                    etlService.refreshSubmissionSummary(message.studentId());
            case AnalyticsRabbitConfig.RK_ATTENDANCE_RECORDED ->
                    etlService.refreshAttendanceSummary(message.studentId());
            case AnalyticsRabbitConfig.RK_FEEDBACK_PUBLISHED ->
                    etlService.refreshFeedbackSummary(message.studentId());
            default -> log.warn("[analytics-etl] 알 수 없는 라우팅 키: {}", routingKey);
        }
    }
}
