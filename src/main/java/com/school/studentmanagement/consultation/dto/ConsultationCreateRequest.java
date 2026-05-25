package com.school.studentmanagement.consultation.dto;

import com.school.studentmanagement.global.enums.ConsultationVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsultationCreateRequest {

    @NotNull(message = "대상 학생 ID는 필수입니다")
    private Long studentId;

    @NotNull(message = "상담 일시는 필수입니다")
    private LocalDateTime consultationDate;

    @NotBlank(message = "상담 내용은 비어있을 수 없습니다")
    private String content;

    // 다음 상담 계획 (선택)
    private String nextPlan;

    // 미지정 시 RESTRICTED(제한적 공개)로 처리
    private ConsultationVisibility visibility;

    @Builder
    public ConsultationCreateRequest(Long studentId, LocalDateTime consultationDate, String content,
                                     String nextPlan, ConsultationVisibility visibility) {
        this.studentId = studentId;
        this.consultationDate = consultationDate;
        this.content = content;
        this.nextPlan = nextPlan;
        this.visibility = visibility;
    }
}
