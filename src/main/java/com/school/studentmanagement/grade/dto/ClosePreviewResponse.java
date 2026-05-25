package com.school.studentmanagement.grade.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ClosePreviewResponse {

    private Integer academicYear;
    private Integer semester;

    private Integer totalMissingCount;     // 채워질 NOT_SUBMITTED row 총 개수
    private Integer affectedStudentCount;  // 영향받을 학생 수

    private List<StudentMissing> students; // 학생별로 묶은 누락 목록

    @Getter
    @Builder
    public static class StudentMissing {
        private Long studentId;
        private String studentName;
        private Integer grade;
        private Integer classNum;
        private Integer studentNum;
        private List<MissingEntry> missing;
    }

    @Getter
    @Builder
    public static class MissingEntry {
        private Long examId;
        private String examName;
        private Long subjectId;
        private String subjectName;
    }
}
