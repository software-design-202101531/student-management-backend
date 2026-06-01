package com.school.studentmanagement.consultation.dto;

import com.school.studentmanagement.global.enums.ConsultationVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsultationUpdateRequest {

    @NotNull(message = "상담 일시는 필수입니다")
    private LocalDateTime consultationDate;

    @NotBlank(message = "상담 내용은 비어있을 수 없습니다")
    @Size(max = 4000, message = "상담 내용은 4000자 이내로 작성해주세요")
    private String content;

    // 빈 문자열 허용 — 기존 nextPlan 을 비우려는 의도로 해석.
    @Size(max = 2000, message = "다음 상담 계획은 2000자 이내로 작성해주세요")
    private String nextPlan;

    @NotNull(message = "공개 범위는 필수입니다")
    private ConsultationVisibility visibility;

    @Builder
    public ConsultationUpdateRequest(LocalDateTime consultationDate, String content,
                                     String nextPlan, ConsultationVisibility visibility) {
        this.consultationDate = consultationDate;
        this.content = content;
        this.nextPlan = nextPlan;
        this.visibility = visibility;
    }
}
