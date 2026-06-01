package com.school.studentmanagement.notification.entity;

import com.school.studentmanagement.global.entity.BaseTimeEntity;
import com.school.studentmanagement.global.enums.NotificationStatus;
import com.school.studentmanagement.global.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notifications",
        indexes = {
                // 미확인 개수 집계 / 본인 알림 목록 조회의 핵심 인덱스
                @Index(name = "idx_notification_recipient_status", columnList = "recipient_user_id, status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 수신자 User PK. 학생/학부모 모두 User이므로 식별자만 보관하고, 조회는 항상 인증 주체의 userId 기준으로 한다.
    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String content;

    // 프론트 딥링크 (예: /students/3/feedbacks). 없으면 null.
    @Column(length = 500)
    private String linkUrl;

    // 연관 자원 PK (성적=examId, 피드백=feedbackId). 의미는 type으로 결정. 클릭 진입/렌더용. null 허용.
    @Column(name = "reference_id")
    private Long referenceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NotificationStatus status;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    // 발생 시각(createdAt)은 BaseTimeEntity(JPA Auditing)가 관리하며, 목록 정렬 기준으로 사용한다.

    @Builder
    private Notification(Long recipientUserId, NotificationType type, String title,
                         String content, String linkUrl, Long referenceId) {
        this.recipientUserId = recipientUserId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.linkUrl = linkUrl;
        this.referenceId = referenceId;
        this.status = NotificationStatus.UNREAD;
    }

    // 확인 처리 (멱등) — 이미 읽음이면 변화 없음
    public void markAsRead() {
        if (this.status == NotificationStatus.UNREAD) {
            this.status = NotificationStatus.READ;
            this.readAt = LocalDateTime.now();
        }
    }

    public boolean isOwnedBy(Long userId) {
        return this.recipientUserId.equals(userId);
    }
}
