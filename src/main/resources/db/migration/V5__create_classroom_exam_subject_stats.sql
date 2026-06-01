-- 학급×시험×과목 점수 분포/통계 사전 집계(P3 분포 OLAP화).
-- ClassroomStatsService.getClassroomStats 가 매 요청마다 student_grades에서 원점수를 끌어와
-- 메모리로 평균/표준편차/분포를 계산하던 것을 이 요약 조회로 대체한다.
--  - 그레인: (classroom_id, exam_id, subject_id). academic_year/semester는 시험 기준 비정규화.
--  - avg/stddev/max/min 은 '원점수' 기준(서비스 기존 동작과 동일), 분포 bin 은 '정규화 점수(raw*100/max)' 기준.
--  - bin0..bin9: [0,10),[10,20),...,[80,90),[90,100] (마지막 bin 은 90 이상 포함, 인덱스 [0,9] 클램프).
CREATE TABLE analytics.classroom_exam_subject_stats (
    classroom_id   BIGINT NOT NULL,
    exam_id        BIGINT NOT NULL,
    subject_id     BIGINT NOT NULL,
    academic_year  INT    NOT NULL,
    semester       INT    NOT NULL,
    student_count  INT    NOT NULL,
    avg_score      DOUBLE PRECISION,
    stddev_score   DOUBLE PRECISION,
    max_raw_score  INT,
    min_raw_score  INT,
    bin0 INT NOT NULL DEFAULT 0,
    bin1 INT NOT NULL DEFAULT 0,
    bin2 INT NOT NULL DEFAULT 0,
    bin3 INT NOT NULL DEFAULT 0,
    bin4 INT NOT NULL DEFAULT 0,
    bin5 INT NOT NULL DEFAULT 0,
    bin6 INT NOT NULL DEFAULT 0,
    bin7 INT NOT NULL DEFAULT 0,
    bin8 INT NOT NULL DEFAULT 0,
    bin9 INT NOT NULL DEFAULT 0,
    updated_at     TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (classroom_id, exam_id, subject_id)
);
