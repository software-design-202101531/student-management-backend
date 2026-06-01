-- 분석 사전 집계(요약) 테이블. 모두 그레인 기준 PK로 멱등 upsert 대상.
-- 이 테이블들은 JPA @Entity로 매핑하지 않으며(ddl-auto 격리), ETL/조회는 JdbcTemplate 순수 SQL로만 접근한다.

-- 학생 × 과목 × 학년도 × 학기 — 가중평균/원점수평균/입력건수
CREATE TABLE analytics.student_subject_summary (
    student_id      BIGINT  NOT NULL,
    subject_id      BIGINT  NOT NULL,
    academic_year   INT     NOT NULL,
    semester        INT     NOT NULL,
    weighted_score  DOUBLE PRECISION,
    avg_raw_score   DOUBLE PRECISION,
    grade_count     INT     NOT NULL,
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (student_id, subject_id, academic_year, semester)
);

-- 학생 × 학년도 × 학기 — 결석/지각/조퇴/총 특이사항(미출결은 운영에서 row 미기록)
CREATE TABLE analytics.student_attendance_summary (
    student_id         BIGINT NOT NULL,
    academic_year      INT    NOT NULL,
    semester           INT    NOT NULL,
    absent_count       INT    NOT NULL,
    late_count         INT    NOT NULL,
    early_leave_count  INT    NOT NULL,
    total_records      INT    NOT NULL,
    updated_at         TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (student_id, academic_year, semester)
);

-- 학생 × 학년도 × 학기 — 발행 피드백 수/공개 수
CREATE TABLE analytics.student_feedback_summary (
    student_id     BIGINT NOT NULL,
    academic_year  INT    NOT NULL,
    semester       INT    NOT NULL,
    total_count    INT    NOT NULL,
    public_count   INT    NOT NULL,
    updated_at     TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (student_id, academic_year, semester)
);

-- 학생 × 과목 × 학년도 × 학기 — 과제 부여/제출/지각/제출률
CREATE TABLE analytics.student_submission_summary (
    student_id       BIGINT NOT NULL,
    subject_id       BIGINT NOT NULL,
    academic_year    INT    NOT NULL,
    semester         INT    NOT NULL,
    assigned_count   INT    NOT NULL,
    submitted_count  INT    NOT NULL,
    late_count       INT    NOT NULL,
    submission_rate  DOUBLE PRECISION,
    updated_at       TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (student_id, subject_id, academic_year, semester)
);

-- 적재 이력
CREATE TABLE analytics.etl_run_log (
    id            BIGSERIAL PRIMARY KEY,
    job_name      VARCHAR(100) NOT NULL,
    started_at    TIMESTAMP NOT NULL,
    finished_at   TIMESTAMP,
    rows_upserted INT,
    status        VARCHAR(20) NOT NULL
);
