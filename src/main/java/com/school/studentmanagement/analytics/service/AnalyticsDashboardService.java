package com.school.studentmanagement.analytics.service;

import com.school.studentmanagement.analytics.dto.StudentAnalyticsOverviewResponse;
import com.school.studentmanagement.analytics.dto.StudentAnalyticsOverviewResponse.AttendanceStat;
import com.school.studentmanagement.analytics.dto.StudentAnalyticsOverviewResponse.FeedbackStat;
import com.school.studentmanagement.analytics.dto.StudentAnalyticsOverviewResponse.SubjectStat;
import com.school.studentmanagement.analytics.dto.StudentAnalyticsOverviewResponse.SubmissionStat;
import com.school.studentmanagement.analytics.dto.SubjectAnalyticsOverviewResponse;
import com.school.studentmanagement.analytics.dto.SubjectAnalyticsOverviewResponse.ScoreDistribution;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

// 대시보드 조회 — analytics 스키마만 JdbcTemplate으로 읽는다(운영 OLTP 미접근).
@Service
@RequiredArgsConstructor
public class AnalyticsDashboardService {

    private final JdbcTemplate jdbc;

    public StudentAnalyticsOverviewResponse getStudentOverview(Long studentId, int academicYear, int semester) {
        List<SubjectStat> subjects = jdbc.query(
                "SELECT subject_id, weighted_score, avg_raw_score, grade_count " +
                        "FROM analytics.student_subject_summary " +
                        "WHERE student_id = ? AND academic_year = ? AND semester = ? ORDER BY subject_id",
                (rs, i) -> SubjectStat.builder()
                        .subjectId(rs.getLong("subject_id"))
                        .weightedScore((Double) rs.getObject("weighted_score"))
                        .avgRawScore((Double) rs.getObject("avg_raw_score"))
                        .gradeCount(rs.getInt("grade_count"))
                        .build(),
                studentId, academicYear, semester);

        AttendanceStat attendance = jdbc.query(
                "SELECT absent_count, late_count, early_leave_count, total_records, school_days, attendance_rate " +
                        "FROM analytics.student_attendance_summary " +
                        "WHERE student_id = ? AND academic_year = ? AND semester = ?",
                (rs, i) -> AttendanceStat.builder()
                        .absentCount(rs.getInt("absent_count"))
                        .lateCount(rs.getInt("late_count"))
                        .earlyLeaveCount(rs.getInt("early_leave_count"))
                        .totalRecords(rs.getInt("total_records"))
                        .schoolDays((Integer) rs.getObject("school_days"))
                        .attendanceRate((Double) rs.getObject("attendance_rate"))
                        .build(),
                studentId, academicYear, semester).stream().findFirst().orElse(null);

        FeedbackStat feedback = jdbc.query(
                "SELECT total_count, public_count FROM analytics.student_feedback_summary " +
                        "WHERE student_id = ? AND academic_year = ? AND semester = ?",
                (rs, i) -> FeedbackStat.builder()
                        .totalCount(rs.getInt("total_count"))
                        .publicCount(rs.getInt("public_count"))
                        .build(),
                studentId, academicYear, semester).stream().findFirst().orElse(null);

        List<SubmissionStat> submissions = jdbc.query(
                "SELECT subject_id, assigned_count, submitted_count, late_count, submission_rate " +
                        "FROM analytics.student_submission_summary " +
                        "WHERE student_id = ? AND academic_year = ? AND semester = ? ORDER BY subject_id",
                (rs, i) -> SubmissionStat.builder()
                        .subjectId(rs.getLong("subject_id"))
                        .assignedCount(rs.getInt("assigned_count"))
                        .submittedCount(rs.getInt("submitted_count"))
                        .lateCount(rs.getInt("late_count"))
                        .submissionRate((Double) rs.getObject("submission_rate"))
                        .build(),
                studentId, academicYear, semester);

        return StudentAnalyticsOverviewResponse.builder()
                .studentId(studentId)
                .academicYear(academicYear)
                .semester(semester)
                .subjects(subjects)
                .attendance(attendance)
                .feedback(feedback)
                .submissions(submissions)
                .build();
    }

    // 과목 단위 학습현황 — 여러 학생을 가로질러 집계(평균/분포/제출률). analytics 스키마만 읽는다.
    public SubjectAnalyticsOverviewResponse getSubjectOverview(Long subjectId, int academicYear, int semester) {
        // 성적 요약 — 학생 수, 가중/원점수 평균, 가중점수 구간 분포를 한 번에 집계
        SubjectAnalyticsOverviewResponse.SubjectAnalyticsOverviewResponseBuilder builder = jdbc.query(
                "SELECT COUNT(*) AS student_count, " +
                        "AVG(weighted_score) AS avg_weighted, AVG(avg_raw_score) AS avg_raw, " +
                        "COUNT(*) FILTER (WHERE weighted_score >= 90) AS b90, " +
                        "COUNT(*) FILTER (WHERE weighted_score >= 80 AND weighted_score < 90) AS b80, " +
                        "COUNT(*) FILTER (WHERE weighted_score >= 70 AND weighted_score < 80) AS b70, " +
                        "COUNT(*) FILTER (WHERE weighted_score >= 60 AND weighted_score < 70) AS b60, " +
                        "COUNT(*) FILTER (WHERE weighted_score < 60) AS b_low " +
                        "FROM analytics.student_subject_summary " +
                        "WHERE subject_id = ? AND academic_year = ? AND semester = ?",
                (rs, i) -> SubjectAnalyticsOverviewResponse.builder()
                        .studentCount(rs.getInt("student_count"))
                        .avgWeightedScore((Double) rs.getObject("avg_weighted"))
                        .avgRawScore((Double) rs.getObject("avg_raw"))
                        .distribution(ScoreDistribution.builder()
                                .range90to100(rs.getInt("b90"))
                                .range80to89(rs.getInt("b80"))
                                .range70to79(rs.getInt("b70"))
                                .range60to69(rs.getInt("b60"))
                                .below60(rs.getInt("b_low"))
                                .build()),
                subjectId, academicYear, semester).stream().findFirst()
                // 집계 함수만 쓰는 쿼리는 항상 1행을 돌려주지만 방어적으로 빈 분포를 둔다
                .orElse(SubjectAnalyticsOverviewResponse.builder()
                        .studentCount(0)
                        .distribution(ScoreDistribution.builder().build()));

        // 과제 제출 요약 — 부여/제출 합계, 학생별 제출률의 평균
        jdbc.query(
                "SELECT COALESCE(SUM(assigned_count), 0) AS assigned_total, " +
                        "COALESCE(SUM(submitted_count), 0) AS submitted_total, " +
                        "AVG(submission_rate) AS avg_rate " +
                        "FROM analytics.student_submission_summary " +
                        "WHERE subject_id = ? AND academic_year = ? AND semester = ?",
                (rs, i) -> {
                    builder.assignedTotal(rs.getInt("assigned_total"))
                            .submittedTotal(rs.getInt("submitted_total"))
                            .avgSubmissionRate((Double) rs.getObject("avg_rate"));
                    return null;
                },
                subjectId, academicYear, semester);

        return builder
                .subjectId(subjectId)
                .academicYear(academicYear)
                .semester(semester)
                .build();
    }
}
