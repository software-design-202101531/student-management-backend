package com.school.studentmanagement.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 행특(subject_id IS NULL)의 학생·학기당 단건을 DB가 강제하도록 <b>부분 유니크 인덱스</b>를 멱등 생성한다.
 * Hibernate {@code @Table(uniqueConstraints)}는 부분(WHERE) 유니크를 표현하지 못해 별도 DDL이 필요하다.
 *
 * <p>이 인덱스는 동시 최초 작성 시 중복 행 생성을 차단하며,
 * {@code StudentRecordRepository.insertBehaviorIfAbsent}의 {@code ON CONFLICT} 충돌 타겟이 된다.</p>
 *
 * <p>dev/test의 {@code ddl-auto: create}는 기동마다 스키마를 재생성하므로 본 러너가 인덱스를 다시 보장한다.
 * 운영에서는 Flyway 등 마이그레이션 도구로 관리하는 것을 권장한다(IF NOT EXISTS라 중복 적용에 안전).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BehaviorRecordIndexInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute(
                "CREATE UNIQUE INDEX IF NOT EXISTS uk_behavior_record " +
                "ON student_records (student_id, academic_year, semester, record_category) " +
                "WHERE subject_id IS NULL");
        log.info("행특 부분 유니크 인덱스(uk_behavior_record) 보장 완료");
    }
}
