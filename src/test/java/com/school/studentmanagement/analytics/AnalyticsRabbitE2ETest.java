package com.school.studentmanagement.analytics;

import com.school.studentmanagement.analytics.event.AnalyticsEventMessage;
import com.school.studentmanagement.analytics.event.AnalyticsRabbitConfig;
import com.school.studentmanagement.analytics.event.AnalyticsSourceEvent;
import com.school.studentmanagement.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * OLAP 이벤트 경로 라이브 e2e (P5). 실제 RabbitMQ 브로커를 띄워, 운영 트랜잭션 커밋 →
 * AnalyticsEventRelay(AFTER_COMMIT) 발행 → Topic Exchange/큐 바인딩 → AnalyticsEtlConsumer 소비 →
 * analytics 요약 upsert 까지 끝에서 끝까지 자동 검증한다.
 * (단위/통합 테스트가 우회하던 토폴로지·바인딩·JSON 직렬화 회귀를 CI가 잡도록 한다.)
 */
@Testcontainers
class AnalyticsRabbitE2ETest extends IntegrationTestSupport {

    // Postgres 는 베이스(IntegrationTestSupport)의 싱글톤을 공유하고, 이 테스트는 RabbitMQ 만 추가로 띄운다.
    @Container
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3-management");

    @DynamicPropertySource
    static void rabbitProps(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
        // 이 테스트만 리스너 기동(기본 test 프로필은 auto-startup=false 로 브로커 의존을 끊는다)
        registry.add("spring.rabbitmq.listener.simple.auto-startup", () -> "true");
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private PlatformTransactionManager txManager;

    @Test
    @DisplayName("e2e: 커밋 후 grade.saved 발행 → 실제 큐 소비 → student_subject_summary upsert")
    void gradeSavedFlowsThroughBrokerToAnalytics() {
        long studentId = 313_131L;
        long subjectId = 323_232L;
        long examId = 330_001L;

        // 원천 데이터(컨슈머가 그레인 재집계 시 읽음). raw 88, max 100, weight 1.0 → 가중점수 88.0
        jdbc.update("INSERT INTO exams (id, academic_year, semester, exam_type, name, max_score, weight, published) " +
                "VALUES (?, 2095, 1, 'MIDTERM', 'e2e테스트', 100, 1.0, true)", examId);
        jdbc.update("INSERT INTO student_grades (id, student_id, exam_id, subject_id, raw_score, attendance_status) " +
                "VALUES (?, ?, ?, ?, 88, 'PRESENT')", 330_101L, studentId, examId, subjectId);

        // 실제 트랜잭션 안에서 이벤트 발행 → 커밋 시 AnalyticsEventRelay 가 RabbitMQ 로 중계
        new TransactionTemplate(txManager).executeWithoutResult(status ->
                eventPublisher.publishEvent(new AnalyticsSourceEvent(
                        AnalyticsRabbitConfig.RK_GRADE_SAVED,
                        new AnalyticsEventMessage(studentId, subjectId))));

        // 컨슈머가 비동기로 처리 → 요약이 적재될 때까지 폴링(최대 20초)
        Double weighted = awaitWeightedScore(studentId, subjectId, Duration.ofSeconds(20));

        assertThat(weighted).isNotNull().isCloseTo(88.0, within(0.001));
    }

    private Double awaitWeightedScore(long studentId, long subjectId, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            List<Double> rows = jdbc.queryForList(
                    "SELECT weighted_score FROM analytics.student_subject_summary " +
                            "WHERE student_id = ? AND subject_id = ? AND academic_year = 2095 AND semester = 1",
                    Double.class, studentId, subjectId);
            if (!rows.isEmpty()) return rows.get(0);
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return null;
    }
}
