package com.school.studentmanagement.notification.dto;

import com.school.studentmanagement.global.enums.NotificationStatus;
import com.school.studentmanagement.global.enums.NotificationType;
import com.school.studentmanagement.notification.entity.Notification;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class NotificationResponse {

    private final Long id;
    private final NotificationType type;
    private final String title;
    private final String content;
    private final String linkUrl;
    private final Long referenceId;
    private final boolean read;
    private final LocalDateTime createdAt;

    private NotificationResponse(Long id, NotificationType type, String title, String content,
                                 String linkUrl, Long referenceId, boolean read, LocalDateTime createdAt) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.content = content;
        this.linkUrl = linkUrl;
        this.referenceId = referenceId;
        this.read = read;
        this.createdAt = createdAt;
    }

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getContent(),
                n.getLinkUrl(),
                n.getReferenceId(),
                n.getStatus() == NotificationStatus.READ,
                n.getCreatedAt()
        );
    }
}
