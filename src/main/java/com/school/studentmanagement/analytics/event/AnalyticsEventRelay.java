package com.school.studentmanagement.analytics.event;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 운영 트랜잭션 커밋 이후(AFTER_COMMIT)에 Spring 이벤트를 RabbitMQ로 중계한다.
 * - 커밋 이후 발행이라 롤백된 변경이 분석으로 새는 것을 막는다(알림에서 검증한 패턴 재사용).
 * - RabbitMQ 발행을 한 곳에 모아 운영 서비스가 메시징에 직접 결합되지 않게 한다.
 *
 * <p><b>전달 보장 = best-effort (의도된 트레이드오프, 버그 아님).</b>
 * 커밋과 발행이 하나의 원자 단위로 묶여 있지 않으므로, 커밋 직후 브로커가 다운 등으로
 * convertAndSend 가 실패하면 해당 이벤트는 유실될 수 있다. 이는 다음 두 안전망으로 흡수한다:
 * <ol>
 *   <li><b>야간 전량 배치</b>({@code AnalyticsEtlScheduler.runDaily})가 매일 전체를 재계산해
 *       유실/지연/순서 문제를 보정한다 — 분석 <i>정확성의 최종 기준</i>은 배치다(이벤트는 신선도용).</li>
 *   <li>모든 적재가 <b>멱등 upsert</b>(그레인 유니크)라 중복 수신에도 안전하다.</li>
 * </ol>
 * 즉 "이벤트=빠른 근실시간, 배치=정확성"의 람다 아키텍처다. 한 건도 잃지 않는 정확 전달이
 * 필요해지면 <b>transactional outbox</b>(아웃박스 테이블 + 폴러)로 격상할 수 있으나, 현 규모에선
 * 과설계로 보류한다(설계 근거: docs/olap-analytics-plan.md §8, docs/olap-remediation-plan.md P6).</p>
 */
@Component
@RequiredArgsConstructor
public class AnalyticsEventRelay {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void relay(AnalyticsSourceEvent event) {
        rabbitTemplate.convertAndSend(AnalyticsRabbitConfig.EXCHANGE, event.routingKey(), event.message());
    }
}
