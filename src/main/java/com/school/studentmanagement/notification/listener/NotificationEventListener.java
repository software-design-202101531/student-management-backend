package com.school.studentmanagement.notification.listener;

import com.school.studentmanagement.assignment.event.AssignmentCreatedEvent;
import com.school.studentmanagement.assignment.event.AssignmentGradedEvent;
import com.school.studentmanagement.feedback.event.FeedbackPublishedEvent;
import com.school.studentmanagement.grade.event.ExamPublishedEvent;
import com.school.studentmanagement.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 발행 트랜잭션이 커밋된 이후(AFTER_COMMIT)에만, 별도 스레드(@Async)에서 알림을 생성한다.
 * - AFTER_COMMIT: 롤백된 발행에 대한 유령 알림을 원천 차단 (dual-write 정합성)
 * - @Async: 교사의 발행 요청 응답을 알림 팬아웃이 지연시키지 않도록 분리
 * NotificationService의 생성 메서드는 @Transactional이라 이 스레드에서 새 트랜잭션으로 저장된다.
 */
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExamPublished(ExamPublishedEvent event) {
        notificationService.createForExamPublished(event.examId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFeedbackPublished(FeedbackPublishedEvent event) {
        notificationService.createForFeedbackPublished(event.feedbackId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAssignmentCreated(AssignmentCreatedEvent event) {
        notificationService.createForAssignmentCreated(event.assignmentId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAssignmentGraded(AssignmentGradedEvent event) {
        notificationService.createForAssignmentGraded(event.submissionId());
    }
}
