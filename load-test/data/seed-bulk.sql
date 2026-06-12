-- 검색·필터링 부하(3.3) 측정용 대용량 더미 데이터 적재.
-- ------------------------------------------------------------------
-- 데모 시드는 2025-2 ~ 2026-1 두 학기뿐이라 "여러 학기 누적" 검색이 가볍게 끝난다.
-- 과거 학년도(2010~2024) 시험과 성적을 대량 생성해 슬로우 쿼리/인덱스 효율을 측정 가능하게 한다.
--
-- 적재(데모 시드가 먼저 올라온 DB 에 추가):
--   docker compose exec -T postgres psql -U "$DB_USER" -d school_db < load-test/data/seed-bulk.sql
--
-- 규모: (2024-2010+1)=15년 × 2학기 × 2시험 = 60 시험,
--       성적 = 60 시험 × 25 학생 × 6 과목 = 9,000 행 (생성 범위를 넓히면 비례 증가).
-- 더 무겁게: 아래 generate_series(2010, 2024) 의 시작 연도를 더 과거로 늘린다.
--
-- ⚠️ 운영 DB 에는 절대 실행하지 말 것. 로컬/스테이징 전용.
-- 스키마 근거: src/main/resources/db/prod-migration/V3__baseline_operational_schema.sql
--   - exams(id IDENTITY, academic_year, semester, exam_type[MIDTERM|FINAL], name(<=50), max_score, weight, published, ...)
--   - student_grades(id IDENTITY, student_id, exam_id, subject_id, raw_score, attendance_status[PRESENT|ABSENT|CHEATED|NOT_SUBMITTED], version)
--     · student_id 는 students.user_id 를 참조(students PK = user_id)
--     · UNIQUE uk_grade_student_exam_subject (student_id, exam_id, subject_id)

BEGIN;

-- 1) 과거 시험 메타: 2010~2024년 × 1·2학기 × (중간/기말). id 는 IDENTITY 가 자동 부여.
INSERT INTO public.exams
    (academic_year, semester, exam_type, name, max_score, weight, published, created_at, updated_at)
SELECT y, s, t.type,
       y || '-' || s || ' ' || t.label,   -- 예: "2010-1 중간고사" (name <= 50자)
       100, 0.5, true, now(), now()
FROM generate_series(2010, 2024) AS y
CROSS JOIN generate_series(1, 2) AS s
CROSS JOIN (VALUES ('MIDTERM', '중간고사'), ('FINAL', '기말고사')) AS t(type, label);

-- 2) 위 시험 × 전체 학생 × 전체 과목 성적 일괄(무작위 점수 0~100, 전원 PRESENT).
--    student_id 는 students.user_id. 기존 데이터와의 유니크 충돌은 무시.
INSERT INTO public.student_grades
    (student_id, exam_id, subject_id, raw_score, attendance_status, version, created_at, updated_at)
SELECT st.user_id, e.id, sub.id,
       floor(random() * 101)::int, 'PRESENT', 0, now(), now()
FROM public.exams e
CROSS JOIN public.students st
CROSS JOIN public.subjects sub
WHERE e.academic_year BETWEEN 2010 AND 2024
ON CONFLICT ON CONSTRAINT uk_grade_student_exam_subject DO NOTHING;

COMMIT;

-- 적재 후 확인:
--   SELECT count(*) FROM public.exams WHERE academic_year BETWEEN 2010 AND 2024;          -- 60 기대
--   SELECT count(*) FROM public.student_grades sg JOIN public.exams e ON e.id = sg.exam_id
--     WHERE e.academic_year BETWEEN 2010 AND 2024;                                         -- 9000 기대
--
-- 검색 부하 실행 시 FROM_YEAR 를 넓혀 누적 효과를 본다:
--   FROM_YEAR=2010 FROM_SEMESTER=1 TO_YEAR=2026 TO_SEMESTER=2 TEST_TYPE=load \
--     k6 run load-test/scenarios/s3_search_filter.js
