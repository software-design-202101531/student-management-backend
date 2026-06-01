package com.school.studentmanagement.analytics.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 성적 분석 조회를 analytics(사전 집계) 스키마에서만 읽는다(P3 — 운영 OLTP 직접 집계 이관).
 * 레이더/추세/학급분포가 매 요청마다 student_grades를 GROUP BY 하던 것을 요약 테이블 조회로 대체한다.
 * 신선도: 이벤트(grade.saved) 증분 + 야간 배치로 갱신되므로 근실시간(정확성은 배치가 보정).
 */
@Repository
@RequiredArgsConstructor
public class AnalyticsGradeQueryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    // 학생 본인의 과목별 학기 가중점수 (subjectId → weightedScore)
    public Map<Long, Double> findStudentSubjectScores(Long studentId, int academicYear, int semester) {
        Map<Long, Double> result = new LinkedHashMap<>();
        jdbc.query(
                "SELECT subject_id, weighted_score FROM analytics.student_subject_summary " +
                        "WHERE student_id = :sid AND academic_year = :y AND semester = :s ORDER BY subject_id",
                new MapSqlParameterSource().addValue("sid", studentId).addValue("y", academicYear).addValue("s", semester),
                rs -> { result.put(rs.getLong("subject_id"), (Double) rs.getObject("weighted_score")); });
        return result;
    }

    // 학급 학생들의 과목별 가중점수 평균 (subjectId → AVG(weightedScore))
    public Map<Long, Double> findClassSubjectAverages(List<Long> studentIds, int academicYear, int semester) {
        Map<Long, Double> result = new LinkedHashMap<>();
        if (studentIds.isEmpty()) return result;
        jdbc.query(
                "SELECT subject_id, AVG(weighted_score) AS avg_score FROM analytics.student_subject_summary " +
                        "WHERE student_id IN (:ids) AND academic_year = :y AND semester = :s " +
                        "GROUP BY subject_id ORDER BY subject_id",
                new MapSqlParameterSource().addValue("ids", studentIds).addValue("y", academicYear).addValue("s", semester),
                rs -> { result.put(rs.getLong("subject_id"), (Double) rs.getObject("avg_score")); });
        return result;
    }

    // 학생의 (과목×학년도×학기) 추세 점수. key = year*10+semester 범위. 과목→(연,학기) 순 정렬.
    public List<TrendRow> findStudentSubjectTrend(Long studentId, int fromKey, int toKey) {
        return jdbc.query(
                "SELECT subject_id, academic_year, semester, weighted_score FROM analytics.student_subject_summary " +
                        "WHERE student_id = :sid AND (academic_year * 10 + semester) BETWEEN :from AND :to " +
                        "ORDER BY subject_id, academic_year, semester",
                new MapSqlParameterSource().addValue("sid", studentId).addValue("from", fromKey).addValue("to", toKey),
                (rs, i) -> new TrendRow(
                        rs.getLong("subject_id"),
                        rs.getInt("academic_year"),
                        rs.getInt("semester"),
                        (Double) rs.getObject("weighted_score")));
    }

    // 학급×시험×과목 분포/통계 1건
    public Optional<ClassroomDistRow> findClassroomExamSubjectStats(Long classroomId, Long examId, Long subjectId) {
        List<ClassroomDistRow> rows = jdbc.query(
                "SELECT student_count, avg_score, stddev_score, max_raw_score, min_raw_score, " +
                        "bin0, bin1, bin2, bin3, bin4, bin5, bin6, bin7, bin8, bin9 " +
                        "FROM analytics.classroom_exam_subject_stats " +
                        "WHERE classroom_id = :c AND exam_id = :e AND subject_id = :s",
                new MapSqlParameterSource().addValue("c", classroomId).addValue("e", examId).addValue("s", subjectId),
                (rs, i) -> new ClassroomDistRow(
                        rs.getInt("student_count"),
                        (Double) rs.getObject("avg_score"),
                        (Double) rs.getObject("stddev_score"),
                        (Integer) rs.getObject("max_raw_score"),
                        (Integer) rs.getObject("min_raw_score"),
                        new int[]{
                                rs.getInt("bin0"), rs.getInt("bin1"), rs.getInt("bin2"), rs.getInt("bin3"), rs.getInt("bin4"),
                                rs.getInt("bin5"), rs.getInt("bin6"), rs.getInt("bin7"), rs.getInt("bin8"), rs.getInt("bin9")
                        }));
        return rows.stream().findFirst();
    }

    public record TrendRow(Long subjectId, int academicYear, int semester, Double score) {}

    public record ClassroomDistRow(int studentCount, Double avgScore, Double stddevScore,
                                   Integer maxRawScore, Integer minRawScore, int[] bins) {}
}
