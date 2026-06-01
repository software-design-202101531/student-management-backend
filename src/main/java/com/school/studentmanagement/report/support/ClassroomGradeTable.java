package com.school.studentmanagement.report.support;

import com.school.studentmanagement.grade.dto.ClassroomGradeResponse;
import com.school.studentmanagement.grade.dto.ClassroomGradeResponse.StudentAllGradesDto;
import com.school.studentmanagement.grade.dto.ClassroomGradeResponse.SubjectScoreDto;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * ClassroomGradeResponse(학생 × 과목)를 표 형태(과목 컬럼 + 학생 행)로 평탄화한다.
 * Excel 작성기와 PDF 템플릿이 동일 구조를 공유하도록 한 곳에서 계산.
 */
@Getter
public class ClassroomGradeTable {

    private final List<String> subjectNames;
    private final List<Row> rows;

    private ClassroomGradeTable(List<String> subjectNames, List<Row> rows) {
        this.subjectNames = subjectNames;
        this.rows = rows;
    }

    public static ClassroomGradeTable from(ClassroomGradeResponse data) {
        List<StudentAllGradesDto> students = data.getStudents() != null ? data.getStudents() : List.of();

        // 과목 컬럼: 전체 학생의 과목명을 등장 순서대로 distinct
        LinkedHashSet<String> subjectSet = new LinkedHashSet<>();
        for (StudentAllGradesDto s : students) {
            for (SubjectScoreDto sc : nullSafe(s.getSubjectScores())) {
                subjectSet.add(sc.getSubjectName());
            }
        }
        List<String> subjectNames = new ArrayList<>(subjectSet);

        List<Row> rows = new ArrayList<>();
        for (StudentAllGradesDto s : students) {
            Map<String, Integer> scoreBySubject = new HashMap<>();
            for (SubjectScoreDto sc : nullSafe(s.getSubjectScores())) {
                if (sc.getRawScore() != null) {
                    scoreBySubject.put(sc.getSubjectName(), sc.getRawScore());
                }
            }
            List<String> scores = new ArrayList<>(subjectNames.size());
            for (String subject : subjectNames) {
                Integer v = scoreBySubject.get(subject);
                scores.add(v != null ? String.valueOf(v) : "");
            }
            rows.add(new Row(
                    s.getStudentNum(), s.getStudentName(), scores,
                    str(s.getTotalScore()), str(s.getAverageScore()),
                    s.getGradeLevel() != null ? s.getGradeLevel().name() : ""));
        }
        return new ClassroomGradeTable(subjectNames, rows);
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list != null ? list : List.of();
    }

    private static String str(Double v) {
        return v != null ? String.valueOf(v) : "";
    }

    @Getter
    public static class Row {
        private final Integer studentNum;
        private final String studentName;
        private final List<String> scores; // subjectNames와 동일 순서, 점수 없으면 ""
        private final String totalScore;
        private final String averageScore;
        private final String gradeLevel;

        Row(Integer studentNum, String studentName, List<String> scores,
            String totalScore, String averageScore, String gradeLevel) {
            this.studentNum = studentNum;
            this.studentName = studentName;
            this.scores = scores;
            this.totalScore = totalScore;
            this.averageScore = averageScore;
            this.gradeLevel = gradeLevel;
        }
    }
}
