package com.school.studentmanagement.grade.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class GradeHistoryResponse {

    private Long gradeId;
    private Long studentId;
    private String studentName;
    private String subjectName;
    private String examName;
    private List<HistoryEntry> histories;

    @Getter
    @Builder
    public static class HistoryEntry {
        private Long historyId;
        private Integer beforeScore;     // 신규 입력이면 null
        private Integer afterScore;
        private Long changedByUserId;
        private String changedByName;
        private String reason;
        private LocalDateTime changedAt;
    }
}
