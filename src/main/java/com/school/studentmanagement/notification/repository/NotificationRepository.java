package com.school.studentmanagement.notification.repository;

import com.school.studentmanagement.global.enums.NotificationStatus;
import com.school.studentmanagement.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 본인 알림 목록 (최신순)
    Page<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId, Pageable pageable);

    // 미확인 개수 (뱃지)
    long countByRecipientUserIdAndStatus(Long recipientUserId, NotificationStatus status);

    // 미확인 → 확인 일괄 처리. clearAutomatically로 영속성 컨텍스트와의 불일치 방지.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n " +
            "SET n.status = com.school.studentmanagement.global.enums.NotificationStatus.READ, n.readAt = :now " +
            "WHERE n.recipientUserId = :userId " +
            "AND n.status = com.school.studentmanagement.global.enums.NotificationStatus.UNREAD")
    int markAllReadByRecipient(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
