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
public class FeedbackUpdateRequest {

    @NotNull(message = "분류는 필수입니다")
    private FeedbackCategory category;

    @NotBlank(message = "피드백 본문은 비어있을 수 없습니다")
    private String content;

    @NotNull(message = "공개 여부는 필수입니다")
    private Boolean isPublic;

    @Builder
    public FeedbackUpdateRequest(FeedbackCategory category, String content, Boolean isPublic) {
        this.category = category;
        this.content = content;
        this.isPublic = isPublic;
    }
}
