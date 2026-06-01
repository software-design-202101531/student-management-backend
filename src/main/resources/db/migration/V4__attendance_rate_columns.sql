-- 출석률 산출용 컬럼 추가(P2). 운영 attendances는 결석/지각/조퇴 '예외'만 저장하고 PRESENT는 미기록이므로
-- total_records로는 출석률을 구할 수 없다. 학사일정(academic_calendars)에서 학기 수업일수를 분모로 적재한다.
--   school_days     : 학기 내 수업일수(월~금 중 비-WEEKDAY 캘린더 제외)
--   attendance_rate : (school_days - absent_count) / school_days  — 지각/조퇴는 출석으로 간주(별도 카운트 유지)
ALTER TABLE analytics.student_attendance_summary
    ADD COLUMN school_days     INT,
    ADD COLUMN attendance_rate DOUBLE PRECISION;
