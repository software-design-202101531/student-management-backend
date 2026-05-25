package com.school.studentmanagement.feedback.dto;

import com.school.studentmanagement.feedback.entity.Feedback;
import com.school.studentmanagement.global.enums.FeedbackCategory;
import com.school.studentmanagement.global.enums.FeedbackStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FeedbackResponse {

    private Long feedbackId;
    private Long studentId;
    private Long teacherId;
    private String teacherName;     // 작성 교사 이름
    private FeedbackCategory category;
    private String categoryLabel;   // 분류 한글 라벨
    private String content;
    private FeedbackStatus status;
    private boolean isPublic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FeedbackResponse from(Feedback feedback) {
        return FeedbackResponse.builder()
                .feedbackId(feedback.getId())
                .studentId(feedback.getStudent().getId())
                .teacherId(feedback.getTeacher().getId())
                .teacherName(feedback.getTeacher().getUser().getName())
                .category(feedback.getCategory())
                .categoryLabel(feedback.getCategory().getDescription())
                .content(feedback.getContent())
                .status(feedback.getStatus())
                .isPublic(feedback.isPublic())
                .createdAt(feedback.getCreatedAt())
                .updatedAt(feedback.getUpdatedAt())
                .build();
    }
}
