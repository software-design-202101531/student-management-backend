package com.school.studentmanagement.analytics.etl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * 운영(public) → 분석(analytics) 배치 ETL.
 * - 분석 테이블은 @Entity로 매핑하지 않으므로 JPA가 아닌 JdbcTemplate 순수 SQL로만 접근(§OLAP 기획 2.1 격리 원칙).
 * - 각 요약은 그레인 PK 기준 `ON CONFLICT DO UPDATE`로 멱등 적재(전량 재계산).
 * - 운영/분석이 같은 DataSource(같은 DB, 다른 스키마)라 단일 트랜잭션으로 처리.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsEtlService {

    private final JdbcTemplate jdbc;

    // 성적: 학생×과목×학기 가중평균/원점수평균/건수 (가중치>0, 응시(raw_score not null)만)
    private static final String SUBJECT_SQL = """
            INSERT INTO analytics.student_subject_summary
              (student_id, subject_id, academic_year, semester, weighted_score, avg_raw_score, grade_count, updated_at)
            SELECT sg.student_id, sg.subject_id, e.academic_year, e.semester,
                   SUM(sg.raw_score * 100.0 / e.max_score * e.weight) / NULLIF(SUM(e.weight), 0),
                   AVG(sg.raw_score),
                   COUNT(*),
                   now()
            FROM student_grades sg
            JOIN exams e ON e.id = sg.exam_id
            WHERE sg.raw_score IS NOT NULL AND e.weight > 0
            GROUP BY sg.student_id, sg.subject_id, e.academic_year, e.semester
            ON CONFLICT (student_id, subject_id, academic_year, semester) DO UPDATE
              SET weighted_score = EXCLUDED.weighted_score,
                  avg_raw_score  = EXCLUDED.avg_raw_score,
                  grade_count    = EXCLUDED.grade_count,
                  updated_at     = now()
            """;

    // 출결: 운영은 결석/지각/조퇴 '예외'만 저장(PRESENT 미기록)하므로 출석률 분모를 학사일정에서 구한다.
    //  - 그레인은 재적(student_affiliations→classroom)의 학년도/학기 — 무결석 학생도 행이 생겨 분모가 완전해진다.
    //  - school_days: 학기 기간(1학기 3~8월 / 2학기 9~익년2월) 중 월~금 & 비-WEEKDAY 캘린더가 아닌 날.
    //  - attendance_rate = (school_days - absent_count)/school_days. 지각/조퇴는 출석 간주(카운트만 유지).
    // __GRAIN_FILTER__=추가 WHERE(그레인 필터). 배치는 빈 문자열, 증분은 " AND sa.student_id = ?".
    private static final String ATTENDANCE_SQL_TEMPLATE = """
            WITH sem AS (
                SELECT DISTINCT c.academic_year AS yr, c.semester AS sm,
                       CASE WHEN c.semester = 1 THEN make_date(c.academic_year, 3, 1)
                            ELSE make_date(c.academic_year, 9, 1) END AS from_date,
                       CASE WHEN c.semester = 1 THEN make_date(c.academic_year, 8, 31)
                            ELSE (make_date(c.academic_year + 1, 3, 1) - 1) END AS to_date
                FROM classrooms c
            ),
            sem_days AS (
                SELECT s.yr, s.sm, s.from_date, s.to_date,
                       COUNT(*) FILTER (
                           WHERE EXTRACT(DOW FROM d.day) BETWEEN 1 AND 5
                             AND NOT EXISTS (SELECT 1 FROM academic_calendars ac
                                             WHERE ac.date = d.day::date AND ac.day_type <> 'WEEKDAY')
                       )::int AS school_days
                FROM sem s
                CROSS JOIN LATERAL generate_series(s.from_date, s.to_date, INTERVAL '1 day') AS d(day)
                GROUP BY s.yr, s.sm, s.from_date, s.to_date
            )
            INSERT INTO analytics.student_attendance_summary
              (student_id, academic_year, semester, absent_count, late_count, early_leave_count,
               total_records, school_days, attendance_rate, updated_at)
            SELECT sa.student_id, c.academic_year, c.semester,
                   COUNT(att.id) FILTER (WHERE att.status = 'ABSENT'),
                   COUNT(att.id) FILTER (WHERE att.status = 'LATE'),
                   COUNT(att.id) FILTER (WHERE att.status = 'EARLY_LEAVE'),
                   COUNT(att.id),
                   sd.school_days,
                   CASE WHEN sd.school_days = 0 THEN NULL
                        ELSE (sd.school_days - COUNT(att.id) FILTER (WHERE att.status = 'ABSENT'))::double precision
                             / sd.school_days END,
                   now()
            FROM student_affiliations sa
            JOIN classrooms c ON c.id = sa.classroom_id
            JOIN sem_days sd ON sd.yr = c.academic_year AND sd.sm = c.semester
            LEFT JOIN attendances att ON att.student_id = sa.student_id
                   AND att.date BETWEEN sd.from_date AND sd.to_date
            WHERE 1 = 1__GRAIN_FILTER__
            GROUP BY sa.student_id, c.academic_year, c.semester, sd.school_days
            ON CONFLICT (student_id, academic_year, semester) DO UPDATE
              SET absent_count      = EXCLUDED.absent_count,
                  late_count        = EXCLUDED.late_count,
                  early_leave_count = EXCLUDED.early_leave_count,
                  total_records     = EXCLUDED.total_records,
                  school_days       = EXCLUDED.school_days,
                  attendance_rate   = EXCLUDED.attendance_rate,
                  updated_at        = now()
            """;

    private static final String ATTENDANCE_SQL = ATTENDANCE_SQL_TEMPLATE.replace("__GRAIN_FILTER__", "");

    // 피드백: 발행 건만, created_at에서 학년도/학기 유도. 총/공개 수
    private static final String FEEDBACK_SQL = """
            INSERT INTO analytics.student_feedback_summary
              (student_id, academic_year, semester, total_count, public_count, updated_at)
            SELECT f.student_id,
                   (CASE WHEN EXTRACT(MONTH FROM f.created_at) >= 3
                         THEN EXTRACT(YEAR FROM f.created_at) ELSE EXTRACT(YEAR FROM f.created_at) - 1 END)::int,
                   (CASE WHEN EXTRACT(MONTH FROM f.created_at) BETWEEN 3 AND 8 THEN 1 ELSE 2 END),
                   COUNT(*),
                   COUNT(*) FILTER (WHERE f.is_public),
                   now()
            FROM feedbacks f
            WHERE f.status = 'PUBLISHED'
            GROUP BY 1, 2, 3
            ON CONFLICT (student_id, academic_year, semester) DO UPDATE
              SET total_count  = EXCLUDED.total_count,
                  public_count = EXCLUDED.public_count,
                  updated_at   = now()
            """;

    // 과제 제출: 학생 소속 학급의 과제(부여) 대비 제출/지각. 학기는 classroom 기준
    private static final String SUBMISSION_SQL = """
            INSERT INTO analytics.student_submission_summary
              (student_id, subject_id, academic_year, semester, assigned_count, submitted_count, late_count, submission_rate, updated_at)
            SELECT sa.student_id, a.subject_id, c.academic_year, c.semester,
                   COUNT(a.id),
                   COUNT(sub.id),
                   COUNT(*) FILTER (WHERE sub.status = 'LATE'),
                   CASE WHEN COUNT(a.id) = 0 THEN 0
                        ELSE COUNT(sub.id)::double precision / COUNT(a.id) END,
                   now()
            FROM student_affiliations sa
            JOIN classrooms c ON c.id = sa.classroom_id
            JOIN assignments a ON a.classroom_id = c.id
            LEFT JOIN submissions sub ON sub.assignment_id = a.id AND sub.student_id = sa.student_id
            GROUP BY sa.student_id, a.subject_id, c.academic_year, c.semester
            ON CONFLICT (student_id, subject_id, academic_year, semester) DO UPDATE
              SET assigned_count  = EXCLUDED.assigned_count,
                  submitted_count = EXCLUDED.submitted_count,
                  late_count      = EXCLUDED.late_count,
                  submission_rate = EXCLUDED.submission_rate,
                  updated_at      = now()
            """;

    // 학급×시험×과목 점수 분포/통계(P3). avg/stddev/max/min=원점수, bin=정규화(raw*100/max) 기준.
    //  - bin 인덱스 = LEAST(9, GREATEST(0, floor(raw*100/max/10))) — 서비스 buildDistribution 과 동일 클램프.
    //  - 학생을 그 시험 학기의 재적 학급으로 매핑(classrooms.academic_year/semester = exam 기준).
    //  - __GRAIN_FILTER__: 배치=빈 문자열, 증분=" AND sg.subject_id = ? AND c.id IN (학생 재적 학급)".
    private static final String CLASSROOM_DIST_SQL_TEMPLATE = """
            INSERT INTO analytics.classroom_exam_subject_stats
              (classroom_id, exam_id, subject_id, academic_year, semester, student_count,
               avg_score, stddev_score, max_raw_score, min_raw_score,
               bin0, bin1, bin2, bin3, bin4, bin5, bin6, bin7, bin8, bin9, updated_at)
            SELECT c.id, sg.exam_id, sg.subject_id, e.academic_year, e.semester,
                   COUNT(*),
                   AVG(sg.raw_score),
                   STDDEV_POP(sg.raw_score),
                   MAX(sg.raw_score), MIN(sg.raw_score),
                   COUNT(*) FILTER (WHERE LEAST(9, GREATEST(0, (FLOOR(sg.raw_score * 100.0 / e.max_score / 10.0))::int)) = 0),
                   COUNT(*) FILTER (WHERE LEAST(9, GREATEST(0, (FLOOR(sg.raw_score * 100.0 / e.max_score / 10.0))::int)) = 1),
                   COUNT(*) FILTER (WHERE LEAST(9, GREATEST(0, (FLOOR(sg.raw_score * 100.0 / e.max_score / 10.0))::int)) = 2),
                   COUNT(*) FILTER (WHERE LEAST(9, GREATEST(0, (FLOOR(sg.raw_score * 100.0 / e.max_score / 10.0))::int)) = 3),
                   COUNT(*) FILTER (WHERE LEAST(9, GREATEST(0, (FLOOR(sg.raw_score * 100.0 / e.max_score / 10.0))::int)) = 4),
                   COUNT(*) FILTER (WHERE LEAST(9, GREATEST(0, (FLOOR(sg.raw_score * 100.0 / e.max_score / 10.0))::int)) = 5),
                   COUNT(*) FILTER (WHERE LEAST(9, GREATEST(0, (FLOOR(sg.raw_score * 100.0 / e.max_score / 10.0))::int)) = 6),
                   COUNT(*) FILTER (WHERE LEAST(9, GREATEST(0, (FLOOR(sg.raw_score * 100.0 / e.max_score / 10.0))::int)) = 7),
                   COUNT(*) FILTER (WHERE LEAST(9, GREATEST(0, (FLOOR(sg.raw_score * 100.0 / e.max_score / 10.0))::int)) = 8),
                   COUNT(*) FILTER (WHERE LEAST(9, GREATEST(0, (FLOOR(sg.raw_score * 100.0 / e.max_score / 10.0))::int)) = 9),
                   now()
            FROM student_grades sg
            JOIN exams e ON e.id = sg.exam_id
            JOIN student_affiliations sa ON sa.student_id = sg.student_id
            JOIN classrooms c ON c.id = sa.classroom_id
                 AND c.academic_year = e.academic_year AND c.semester = e.semester
            WHERE sg.raw_score IS NOT NULL AND e.max_score > 0__GRAIN_FILTER__
            GROUP BY c.id, sg.exam_id, sg.subject_id, e.academic_year, e.semester
            ON CONFLICT (classroom_id, exam_id, subject_id) DO UPDATE
              SET academic_year = EXCLUDED.academic_year, semester = EXCLUDED.semester,
                  student_count = EXCLUDED.student_count, avg_score = EXCLUDED.avg_score,
                  stddev_score = EXCLUDED.stddev_score, max_raw_score = EXCLUDED.max_raw_score,
                  min_raw_score = EXCLUDED.min_raw_score,
                  bin0 = EXCLUDED.bin0, bin1 = EXCLUDED.bin1, bin2 = EXCLUDED.bin2, bin3 = EXCLUDED.bin3,
                  bin4 = EXCLUDED.bin4, bin5 = EXCLUDED.bin5, bin6 = EXCLUDED.bin6, bin7 = EXCLUDED.bin7,
                  bin8 = EXCLUDED.bin8, bin9 = EXCLUDED.bin9, updated_at = now()
            """;

    private static final String CLASSROOM_DIST_SQL = CLASSROOM_DIST_SQL_TEMPLATE.replace("__GRAIN_FILTER__", "");

    private static final String CLASSROOM_DIST_GRAIN_SQL = CLASSROOM_DIST_SQL_TEMPLATE.replace(
            "__GRAIN_FILTER__",
            " AND sg.subject_id = ? AND c.id IN (SELECT sa2.classroom_id FROM student_affiliations sa2 WHERE sa2.student_id = ?)");

    // 전량 재적재 후, 이번 실행에서 갱신되지 않은(=원천이 사라진) 고아 요약을 제거하는 테이블들.
    // Postgres now()는 트랜잭션 시작 시각 고정이라, 이번 트랜잭션의 모든 upsert는 동일 updated_at을 갖는다.
    // → updated_at < (트랜잭션 시각)인 행은 이번 적재에서 손대지 않은 stale 행이다(P4).
    private static final String[] SUMMARY_TABLES = {
            "analytics.student_subject_summary",
            "analytics.student_attendance_summary",
            "analytics.student_feedback_summary",
            "analytics.student_submission_summary",
            "analytics.classroom_exam_subject_stats",
    };

    @Transactional
    public int runAll() {
        LocalDateTime startedAt = LocalDateTime.now();
        // 트랜잭션 시작 시각(DB 시계, 컬럼과 동일한 timestamp-without-tz로 캐스팅).
        // 모든 upsert의 updated_at(now()) 과 정확히 일치 → 이보다 과거면 이번 적재에서 손대지 않은 고아.
        Timestamp txnNow = jdbc.queryForObject("SELECT now()::timestamp", Timestamp.class);

        int rows = 0;
        rows += jdbc.update(SUBJECT_SQL);
        rows += jdbc.update(ATTENDANCE_SQL);
        rows += jdbc.update(FEEDBACK_SQL);
        rows += jdbc.update(SUBMISSION_SQL);
        rows += jdbc.update(CLASSROOM_DIST_SQL);

        // 고아(stale) 요약 정리 — 원천 삭제/정정으로 더 이상 산출되지 않는 그레인 제거(수치 과대표기 방지).
        int deleted = 0;
        for (String table : SUMMARY_TABLES) {
            deleted += jdbc.update("DELETE FROM " + table + " WHERE updated_at < ?", txnNow);
        }

        jdbc.update("INSERT INTO analytics.etl_run_log(job_name, started_at, finished_at, rows_upserted, status) " +
                        "VALUES (?, ?, now(), ?, 'SUCCESS')",
                "full", Timestamp.valueOf(startedAt), rows);

        log.info("[analytics-etl] 적재 완료 — upserted rows={}, stale 제거={}", rows, deleted);
        return rows;
    }

    // ===== 이벤트 기반 증분: 영향받은 학생(±과목) 그레인만 원천에서 재집계(멱등 upsert) =====
    // 배치 SQL과 동일 로직 + 그레인 WHERE 필터. (삭제로 원천이 0행이 된 그레인은 갱신되지 않음 — 야간 배치가 보정)

    private static final String SUBJECT_GRAIN_SQL = """
            INSERT INTO analytics.student_subject_summary
              (student_id, subject_id, academic_year, semester, weighted_score, avg_raw_score, grade_count, updated_at)
            SELECT sg.student_id, sg.subject_id, e.academic_year, e.semester,
                   SUM(sg.raw_score * 100.0 / e.max_score * e.weight) / NULLIF(SUM(e.weight), 0),
                   AVG(sg.raw_score), COUNT(*), now()
            FROM student_grades sg JOIN exams e ON e.id = sg.exam_id
            WHERE sg.raw_score IS NOT NULL AND e.weight > 0 AND sg.student_id = ? AND sg.subject_id = ?
            GROUP BY sg.student_id, sg.subject_id, e.academic_year, e.semester
            ON CONFLICT (student_id, subject_id, academic_year, semester) DO UPDATE
              SET weighted_score = EXCLUDED.weighted_score, avg_raw_score = EXCLUDED.avg_raw_score,
                  grade_count = EXCLUDED.grade_count, updated_at = now()
            """;

    // 증분: 한 학생의 출결 요약만 재집계(배치와 동일 로직 + 학생 필터). school_days/출석률 동일 산출.
    private static final String ATTENDANCE_GRAIN_SQL =
            ATTENDANCE_SQL_TEMPLATE.replace("__GRAIN_FILTER__", " AND sa.student_id = ?");

    private static final String FEEDBACK_GRAIN_SQL = """
            INSERT INTO analytics.student_feedback_summary
              (student_id, academic_year, semester, total_count, public_count, updated_at)
            SELECT f.student_id,
                   (CASE WHEN EXTRACT(MONTH FROM f.created_at) >= 3
                         THEN EXTRACT(YEAR FROM f.created_at) ELSE EXTRACT(YEAR FROM f.created_at) - 1 END)::int,
                   (CASE WHEN EXTRACT(MONTH FROM f.created_at) BETWEEN 3 AND 8 THEN 1 ELSE 2 END),
                   COUNT(*), COUNT(*) FILTER (WHERE f.is_public), now()
            FROM feedbacks f
            WHERE f.status = 'PUBLISHED' AND f.student_id = ?
            GROUP BY 1, 2, 3
            ON CONFLICT (student_id, academic_year, semester) DO UPDATE
              SET total_count = EXCLUDED.total_count, public_count = EXCLUDED.public_count, updated_at = now()
            """;

    private static final String SUBMISSION_GRAIN_SQL = """
            INSERT INTO analytics.student_submission_summary
              (student_id, subject_id, academic_year, semester, assigned_count, submitted_count, late_count, submission_rate, updated_at)
            SELECT sa.student_id, a.subject_id, c.academic_year, c.semester,
                   COUNT(a.id), COUNT(sub.id), COUNT(*) FILTER (WHERE sub.status = 'LATE'),
                   CASE WHEN COUNT(a.id) = 0 THEN 0 ELSE COUNT(sub.id)::double precision / COUNT(a.id) END, now()
            FROM student_affiliations sa
            JOIN classrooms c ON c.id = sa.classroom_id
            JOIN assignments a ON a.classroom_id = c.id
            LEFT JOIN submissions sub ON sub.assignment_id = a.id AND sub.student_id = sa.student_id
            WHERE sa.student_id = ?
            GROUP BY sa.student_id, a.subject_id, c.academic_year, c.semester
            ON CONFLICT (student_id, subject_id, academic_year, semester) DO UPDATE
              SET assigned_count = EXCLUDED.assigned_count, submitted_count = EXCLUDED.submitted_count,
                  late_count = EXCLUDED.late_count, submission_rate = EXCLUDED.submission_rate, updated_at = now()
            """;

    @Transactional
    public void refreshSubjectSummary(Long studentId, Long subjectId) {
        jdbc.update(SUBJECT_GRAIN_SQL, studentId, subjectId);
    }

    @Transactional
    public void refreshAttendanceSummary(Long studentId) {
        jdbc.update(ATTENDANCE_GRAIN_SQL, studentId);
    }

    @Transactional
    public void refreshFeedbackSummary(Long studentId) {
        jdbc.update(FEEDBACK_GRAIN_SQL, studentId);
    }

    @Transactional
    public void refreshSubmissionSummary(Long studentId) {
        jdbc.update(SUBMISSION_GRAIN_SQL, studentId);
    }

    // 성적 변경 시 해당 학생이 속한 학급의 (시험×과목) 분포를 재집계(학급 전체 그레인을 다시 계산).
    @Transactional
    public void refreshClassroomDistribution(Long studentId, Long subjectId) {
        jdbc.update(CLASSROOM_DIST_GRAIN_SQL, subjectId, studentId);
    }
}
