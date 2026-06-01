package com.school.studentmanagement.analytics.event;

import com.school.studentmanagement.analytics.etl.AnalyticsEtlService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

// 라우팅 키 → 올바른 그레인 refresh 디스패치 검증 (브로커 불필요)
@ExtendWith(MockitoExtension.class)
class AnalyticsEtlConsumerTest {

    @InjectMocks private AnalyticsEtlConsumer consumer;
    @Mock private AnalyticsEtlService etlService;

    @Test
    void gradeSaved_refreshesSubjectSummary() {
        consumer.onEvent(new AnalyticsEventMessage(1L, 10L), AnalyticsRabbitConfig.RK_GRADE_SAVED);
        verify(etlService).refreshSubjectSummary(1L, 10L);
    }

    @Test
    void submissionCreated_refreshesSubmissionSummary() {
        consumer.onEvent(new AnalyticsEventMessage(2L, null), AnalyticsRabbitConfig.RK_SUBMISSION_CREATED);
        verify(etlService).refreshSubmissionSummary(2L);
    }

    @Test
    void attendanceRecorded_refreshesAttendanceSummary() {
        consumer.onEvent(new AnalyticsEventMessage(3L, null), AnalyticsRabbitConfig.RK_ATTENDANCE_RECORDED);
        verify(etlService).refreshAttendanceSummary(3L);
    }

    @Test
    void feedbackPublished_refreshesFeedbackSummary() {
        consumer.onEvent(new AnalyticsEventMessage(4L, null), AnalyticsRabbitConfig.RK_FEEDBACK_PUBLISHED);
        verify(etlService).refreshFeedbackSummary(4L);
    }

    @Test
    void unknownRoutingKey_noOp() {
        consumer.onEvent(new AnalyticsEventMessage(5L, null), "unknown.key");
        verifyNoInteractions(etlService);
    }
}
