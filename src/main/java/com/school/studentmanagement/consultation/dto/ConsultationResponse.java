package com.school.studentmanagement.consultation.dto;

import com.school.studentmanagement.consultation.entity.Consultation;
import com.school.studentmanagement.global.enums.ConsultationVisibility;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ConsultationResponse {

    private Long consultationId;
    private Long studentId;
    private Long teacherId;
    private String teacherName;     // 작성 교사 이름
    private LocalDateTime consultationDate;
    private String content;
    private String nextPlan;
    private ConsultationVisibility visibility;
    private String visibilityLabel; // 공개 범위 한글 라벨
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ConsultationResponse from(Consultation consultation) {
        return ConsultationResponse.builder()
                .consultationId(consultation.getId())
                .studentId(consultation.getStudent().getId())
                .teacherId(consultation.getTeacher().getId())
                .teacherName(consultation.getTeacher().getUser().getName())
                .consultationDate(consultation.getConsultationDate())
                .content(consultation.getContent())
                .nextPlan(consultation.getNextPlan())
                .visibility(consultation.getVisibility())
                .visibilityLabel(consultation.getVisibility().getDescription())
                .createdAt(consultation.getCreatedAt())
                .updatedAt(consultation.getUpdatedAt())
                .build();
    }
}
