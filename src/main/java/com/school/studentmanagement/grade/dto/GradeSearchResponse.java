package com.school.studentmanagement.grade.dto;

import com.school.studentmanagement.global.enums.ExamAttendanceStatus;
import com.school.studentmanagement.global.enums.ExamType;
import com.school.studentmanagement.grade.entity.StudentGrade;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

// 성적 검색 결과 — 한 학생의 개별 시험 성적 행 목록 (학기 desc, 시험일 desc 정렬)
@Getter
@Builder
public class GradeSearchResponse {

    private Long studentId;
    private int count;
    private List<Item> grades;

    public static GradeSearchResponse from(Long studentId, List<StudentGrade> grades) {
        List<Item> items = grades.stream().map(Item::from).toList();
        return GradeSearchResponse.builder()
                .studentId(studentId)
                .count(items.size())
                .grades(items)
                .build();
    }

    @Getter
    @Builder
    public static class Item {
        private Long gradeId;
        private Long examId;
        private Integer academicYear;
        private Integer semester;
        private ExamType examType;
        private String examName;
        private LocalDate examDate;
        private boolean published;
        private Long subjectId;
        private String subjectName;
        private Integer rawScore;                        // ABSENT/미입력이면 null
        private Integer maxScore;
        private ExamAttendanceStatus attendanceStatus;

        private static Item from(StudentGrade sg) {
            return Item.builder()
                    .gradeId(sg.getId())
                    .examId(sg.getExam().getId())
                    .academicYear(sg.getExam().getAcademicYear())
                    .semester(sg.getExam().getSemester())
                    .examType(sg.getExam().getExamType())
                    .examName(sg.getExam().getName())
                    .examDate(sg.getExam().getExamDate())
                    .published(sg.getExam().isPublished())
                    .subjectId(sg.getSubject().getId())
                    .subjectName(sg.getSubject().getName())
                    .rawScore(sg.getRawScore())
                    .maxScore(sg.getExam().getMaxScore())
                    .attendanceStatus(sg.getAttendanceStatus())
                    .build();
        }
    }
}
