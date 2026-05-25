package com.school.studentmanagement.feedback.dto;

import com.school.studentmanagement.global.enums.FeedbackCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedbackCreateRequest {

    @NotNull(message = "대상 학생 ID는 필수입니다")
    private Long studentId;

    @NotNull(message = "분류는 필수입니다")
    private FeedbackCategory category;

    @NotBlank(message = "피드백 본문은 비어있을 수 없습니다")
    private String content;

    // 미지정 시 비공개(false)로 처리
    private Boolean isPublic;

    @Builder
    public FeedbackCreateRequest(Long studentId, FeedbackCategory category, String content, Boolean isPublic) {
        this.studentId = studentId;
        this.category = category;
        this.content = content;
        this.isPublic = isPublic;
    }
}
