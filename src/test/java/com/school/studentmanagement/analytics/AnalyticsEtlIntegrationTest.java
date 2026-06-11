package com.school.studentmanagement.analytics;

import com.school.studentmanagement.analytics.dto.StudentAnalyticsOverviewResponse;
import com.school.studentmanagement.analytics.dto.SubjectAnalyticsOverviewResponse;
import com.school.studentmanagement.analytics.etl.AnalyticsEtlService;
import com.school.studentmanagement.analytics.repository.AnalyticsGradeQueryRepository;
import com.school.studentmanagement.analytics.service.AnalyticsDashboardService;
import com.school.studentmanagement.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * OLAP Phase 0~1 통합 테스트.
 * - Flyway가 analytics 스키마/테이블을 생성하고(Phase 0),
 * - 배치 ETL 4종 SQL이 무오류로 실행되며(SQL 정합성),
 * - 운영(student_grades) → 분석(student_subject_summary) 적재가 동작하고,
 * - 대시보드 조회가 analytics만 읽어 결과를 반환함을 검증한다.
 * (FK는 test 커넥션의 session_replication_role='replica'로 우회 — 부모행 없이 직접 삽입)
 */
class AnalyticsEtlIntegrationTest extends IntegrationTestSupport {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private AnalyticsEtlService etlService;
    @Autowired private AnalyticsDashboardService dashboardService;
    @Autowired private AnalyticsGradeQueryRepository gradeQueryRepository;

    @Test
    @DisplayName("ETL: 성적 → student_subject_summary 적재 + 4종 SQL 무오류 + 대시보드 조회")
    void etlPopulatesSubjectSummaryAndDashboard() {
        long studentId = 777_777L;
        long subjectId = 778_888L;
        long examId = 770_001L;
        long gradeId = 770_001L;

        // 운영 데이터 직접 삽입 (FK 우회). 가중치 1.0, 만점 100, 원점수 90 → 가중점수 90.0
        jdbc.update("INSERT INTO exams (id, academic_year, semester, exam_type, name, max_score, weight, published) " +
                "VALUES (?, 2026, 1, 'MIDTERM', '분석테스트', 100, 1.0, true)", examId);
        jdbc.update("INSERT INTO student_grades (id, student_id, exam_id, subject_id, raw_score, attendance_status) " +
                "VALUES (?, ?, ?, ?, 90, 'PRESENT')", gradeId, studentId, examId, subjectId);

        // ETL 실행 (4종 upsert 모두 실행 — 미오류 = SQL 정합성 확인)
        int rows = etlService.runAll();
        assertThat(rows).isGreaterThanOrEqualTo(1);

        // 적재 결과: 가중점수 90.0
        Double weighted = jdbc.queryForObject(
                "SELECT weighted_score FROM analytics.student_subject_summary " +
                        "WHERE student_id = ? AND subject_id = ? AND academic_year = 2026 AND semester = 1",
                Double.class, studentId, subjectId);
        assertThat(weighted).isCloseTo(90.0, within(0.001));

        // 적재 이력 기록
        Integer successLogs = jdbc.queryForObject(
                "SELECT count(*) FROM analytics.etl_run_log WHERE status = 'SUCCESS'", Integer.class);
        assertThat(successLogs).isGreaterThanOrEqualTo(1);

        // 대시보드는 analytics만 읽어 반환
        StudentAnalyticsOverviewResponse overview = dashboardService.getStudentOverview(studentId, 2026, 1);
        assertThat(overview.getSubjects())
                .anySatisfy(s -> {
                    assertThat(s.getSubjectId()).isEqualTo(subjectId);
                    assertThat(s.getWeightedScore()).isCloseTo(90.0, within(0.001));
                    assertThat(s.getGradeCount()).isEqualTo(1);
                });
    }

    @Test
    @DisplayName("증분 refresh: 한 그레인만 재집계해 upsert (이벤트 소비 경로)")
    void incrementalRefreshSubjectSummary() {
        long studentId = 888_888L;
        long subjectId = 999_999L;
        long examId = 880_001L;

        jdbc.update("INSERT INTO exams (id, academic_year, semester, exam_type, name, max_score, weight, published) " +
                "VALUES (?, 2026, 1, 'FINAL', '증분테스트', 100, 1.0, true)", examId);
        jdbc.update("INSERT INTO student_grades (id, student_id, exam_id, subject_id, raw_score, attendance_status) " +
                "VALUES (?, ?, ?, ?, 80, 'PRESENT')", 880_001L, studentId, examId, subjectId);

        // 컨슈머가 호출하는 그레인 단위 메서드를 직접 검증
        etlService.refreshSubjectSummary(studentId, subjectId);

        Double weighted = jdbc.queryForObject(
                "SELECT weighted_score FROM analytics.student_subject_summary " +
                        "WHERE student_id = ? AND subject_id = ? AND academic_year = 2026 AND semester = 1",
                Double.class, studentId, subjectId);
        assertThat(weighted).isCloseTo(80.0, within(0.001));
    }

    @Test
    @DisplayName("출결 그레인 refresh: 재적(학급) 기준 학기 + 결석/총계 + 수업일수/출석률 (attendance.recorded 소비 경로)")
    void refreshAttendanceSummaryComputesRate() {
        long studentId = 666_666L;
        long classroomId = 661_000L;
        // 시드와 충돌하지 않도록 미래 학년도(2099) 학급에 재적 → 출결 요약 그레인이 학급의 학년도/학기를 따른다
        jdbc.update("INSERT INTO classrooms (id, academic_year, semester, grade, class_num) " +
                "VALUES (?, 2099, 1, 9, 9)", classroomId);
        jdbc.update("INSERT INTO student_affiliations (id, student_id, classroom_id, student_num) " +
                "VALUES (?, ?, ?, 1)", 661_001L, studentId, classroomId);
        // 2099-04-10(1학기 범위) 결석 1건
        jdbc.update("INSERT INTO attendances (id, student_id, teacher_id, date, status) " +
                "VALUES (?, ?, ?, DATE '2099-04-10', 'ABSENT')", 660_001L, studentId, 660_009L);

        etlService.refreshAttendanceSummary(studentId);

        Integer absent = jdbc.queryForObject(
                "SELECT absent_count FROM analytics.student_attendance_summary " +
                        "WHERE student_id = ? AND academic_year = 2099 AND semester = 1",
                Integer.class, studentId);
        Integer schoolDays = jdbc.queryForObject(
                "SELECT school_days FROM analytics.student_attendance_summary " +
                        "WHERE student_id = ? AND academic_year = 2099 AND semester = 1",
                Integer.class, studentId);
        Double rate = jdbc.queryForObject(
                "SELECT attendance_rate FROM analytics.student_attendance_summary " +
                        "WHERE student_id = ? AND academic_year = 2099 AND semester = 1",
                Double.class, studentId);
        assertThat(absent).isEqualTo(1);
        // 1학기(3/1~8/31)의 평일 수(휴일 캘린더 없음) — 양수여야 분모로 유효
        assertThat(schoolDays).isNotNull().isGreaterThan(100);
        // 출석률 = (수업일 - 결석1)/수업일
        assertThat(rate).isCloseTo((schoolDays - 1.0) / schoolDays, within(0.0001));
    }

    @Test
    @DisplayName("무결석 재적 학생도 출결 요약 행이 생성된다(분모 완전성)")
    void perfectAttendanceStudentStillGetsRow() {
        long studentId = 663_000L;
        long classroomId = 663_100L;
        // 다른 미래 학년도(2098)로 시드/타 테스트와 충돌 회피
        jdbc.update("INSERT INTO classrooms (id, academic_year, semester, grade, class_num) " +
                "VALUES (?, 2098, 1, 9, 9)", classroomId);
        jdbc.update("INSERT INTO student_affiliations (id, student_id, classroom_id, student_num) " +
                "VALUES (?, ?, ?, 1)", 663_101L, studentId, classroomId);
        // 출결 예외 행 없음(무결석)

        etlService.refreshAttendanceSummary(studentId);

        Double rate = jdbc.queryForObject(
                "SELECT attendance_rate FROM analytics.student_attendance_summary " +
                        "WHERE student_id = ? AND academic_year = 2098 AND semester = 1",
                Double.class, studentId);
        Integer absent = jdbc.queryForObject(
                "SELECT absent_count FROM analytics.student_attendance_summary " +
                        "WHERE student_id = ? AND academic_year = 2098 AND semester = 1",
                Integer.class, studentId);
        assertThat(absent).isEqualTo(0);
        assertThat(rate).isCloseTo(1.0, within(0.0001)); // 결석 0 → 출석률 100%
    }

    @Test
    @DisplayName("과목 대시보드: 여러 학생 가로질러 평균/분포 집계")
    void subjectOverviewAggregatesAcrossStudents() {
        long subjectId = 555_001L;
        // analytics 요약에 직접 적재(대시보드는 analytics만 읽음) — 세 학생의 가중점수
        for (Object[] row : new Object[][]{{501L, 95.0}, {502L, 85.0}, {503L, 55.0}}) {
            jdbc.update("INSERT INTO analytics.student_subject_summary " +
                    "(student_id, subject_id, academic_year, semester, weighted_score, avg_raw_score, grade_count) " +
                    "VALUES (?, ?, 2026, 1, ?, ?, 1)", row[0], subjectId, row[1], row[1]);
        }

        SubjectAnalyticsOverviewResponse overview = dashboardService.getSubjectOverview(subjectId, 2026, 1);

        assertThat(overview.getStudentCount()).isEqualTo(3);
        assertThat(overview.getAvgWeightedScore()).isCloseTo((95.0 + 85.0 + 55.0) / 3, within(0.001));
        assertThat(overview.getDistribution().getRange90to100()).isEqualTo(1);
        assertThat(overview.getDistribution().getRange80to89()).isEqualTo(1);
        assertThat(overview.getDistribution().getBelow60()).isEqualTo(1);
    }

    @Test
    @DisplayName("분포 ETL(P3): 학급×시험×과목 평균/표준편차/분포 사전 집계 + 조회 repo")
    void classroomExamSubjectDistributionEtl() {
        long classroomId = 911_000L;
        long examId = 911_100L;
        long subjectId = 911_200L;
        long s1 = 911_301L, s2 = 911_302L;
        // 2097학년도 1학기 학급에 두 학생 재적
        jdbc.update("INSERT INTO classrooms (id, academic_year, semester, grade, class_num) VALUES (?, 2097, 1, 9, 9)", classroomId);
        jdbc.update("INSERT INTO student_affiliations (id, student_id, classroom_id, student_num) VALUES (?, ?, ?, 1)", 911_401L, s1, classroomId);
        jdbc.update("INSERT INTO student_affiliations (id, student_id, classroom_id, student_num) VALUES (?, ?, ?, 2)", 911_402L, s2, classroomId);
        jdbc.update("INSERT INTO exams (id, academic_year, semester, exam_type, name, max_score, weight, published) " +
                "VALUES (?, 2097, 1, 'MIDTERM', '분포테스트', 100, 1.0, true)", examId);
        // 원점수 95, 55 → avg 75, max 95, min 55, stddev_pop 20, bin9=1(95), bin5=1(55)
        jdbc.update("INSERT INTO student_grades (id, student_id, exam_id, subject_id, raw_score, attendance_status) VALUES (?, ?, ?, ?, 95, 'PRESENT')", 911_501L, s1, examId, subjectId);
        jdbc.update("INSERT INTO student_grades (id, student_id, exam_id, subject_id, raw_score, attendance_status) VALUES (?, ?, ?, ?, 55, 'PRESENT')", 911_502L, s2, examId, subjectId);

        etlService.runAll();

        AnalyticsGradeQueryRepository.ClassroomDistRow row = gradeQueryRepository
                .findClassroomExamSubjectStats(classroomId, examId, subjectId).orElseThrow();
        assertThat(row.studentCount()).isEqualTo(2);
        assertThat(row.avgScore()).isCloseTo(75.0, within(0.001));
        assertThat(row.stddevScore()).isCloseTo(20.0, within(0.001));
        assertThat(row.maxRawScore()).isEqualTo(95);
        assertThat(row.minRawScore()).isEqualTo(55);
        assertThat(row.bins()[9]).isEqualTo(1); // 95점 → 90-100 구간
        assertThat(row.bins()[5]).isEqualTo(1); // 55점 → 50-59 구간
    }

    @Test
    @DisplayName("레이더 조회 repo(P3): 본인 과목점수 + 학급 과목평균을 analytics에서 읽는다")
    void radarReadsFromAnalytics() {
        long me = 922_001L, mate = 922_002L;
        long subA = 922_101L, subB = 922_102L;
        // analytics 요약에 직접 적재(이미 적재된 상태를 가정한 조회 검증)
        jdbc.update("INSERT INTO analytics.student_subject_summary (student_id, subject_id, academic_year, semester, weighted_score, avg_raw_score, grade_count) VALUES (?, ?, 2097, 1, 90, 90, 1)", me, subA);
        jdbc.update("INSERT INTO analytics.student_subject_summary (student_id, subject_id, academic_year, semester, weighted_score, avg_raw_score, grade_count) VALUES (?, ?, 2097, 1, 70, 70, 1)", me, subB);
        jdbc.update("INSERT INTO analytics.student_subject_summary (student_id, subject_id, academic_year, semester, weighted_score, avg_raw_score, grade_count) VALUES (?, ?, 2097, 1, 50, 50, 1)", mate, subA);

        var myScores = gradeQueryRepository.findStudentSubjectScores(me, 2097, 1);
        assertThat(myScores).containsEntry(subA, 90.0).containsEntry(subB, 70.0);

        // 학급(me+mate) subA 평균 = (90+50)/2 = 70
        var classAvg = gradeQueryRepository.findClassSubjectAverages(java.util.List.of(me, mate), 2097, 1);
        assertThat(classAvg.get(subA)).isCloseTo(70.0, within(0.001));
        assertThat(classAvg.get(subB)).isCloseTo(70.0, within(0.001)); // mate는 subB 없음 → me만
    }

    @Test
    @DisplayName("추세 조회 repo(P3): 학기 범위의 과목별 점수를 시간순으로 읽는다")
    void trendReadsFromAnalytics() {
        long st = 933_001L;
        long sub = 933_101L;
        jdbc.update("INSERT INTO analytics.student_subject_summary (student_id, subject_id, academic_year, semester, weighted_score, avg_raw_score, grade_count) VALUES (?, ?, 2096, 1, 60, 60, 1)", st, sub);
        jdbc.update("INSERT INTO analytics.student_subject_summary (student_id, subject_id, academic_year, semester, weighted_score, avg_raw_score, grade_count) VALUES (?, ?, 2096, 2, 80, 80, 1)", st, sub);

        // fromKey=2096*10+1=20961, toKey=2096*10+2=20962
        var rows = gradeQueryRepository.findStudentSubjectTrend(st, 20961, 20962);
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).semester()).isEqualTo(1);
        assertThat(rows.get(0).score()).isCloseTo(60.0, within(0.001));
        assertThat(rows.get(1).semester()).isEqualTo(2);
        assertThat(rows.get(1).score()).isCloseTo(80.0, within(0.001));
    }

    @Test
    @DisplayName("배치 runAll: 원천이 사라진 고아 요약을 제거한다(P4)")
    void runAllPurgesStaleSummaries() {
        long staleStudent = 444_001L;
        long staleSubject = 444_002L;
        // 원천(student_grades)에는 없고 analytics 요약에만 남은 고아 행을 과거 시각으로 심는다
        jdbc.update("INSERT INTO analytics.student_subject_summary " +
                "(student_id, subject_id, academic_year, semester, weighted_score, avg_raw_score, grade_count, updated_at) " +
                "VALUES (?, ?, 2026, 1, 70, 70, 1, now() - INTERVAL '1 day')", staleStudent, staleSubject);

        etlService.runAll();

        Integer remaining = jdbc.queryForObject(
                "SELECT COUNT(*) FROM analytics.student_subject_summary WHERE student_id = ?",
                Integer.class, staleStudent);
        assertThat(remaining).isZero();
    }
}
