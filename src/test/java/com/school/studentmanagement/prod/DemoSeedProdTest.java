package com.school.studentmanagement.prod;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 배포(prod) 환경에서 데모 시딩이 정상 동작하는지 검증한다.
 *
 * <p>{@link ProdSchemaValidationTest} 와 동일하게 prod 프로필 + Flyway(V3 baseline) + ddl-auto:validate 조건이되,
 * {@code app.demo.enabled=true} 를 강제해 {@code DemoDataInitializer} 가 실제로 시딩 + 분석 ETL을 수행하게 한다.</p>
 *
 * <p>이 테스트가 통과하면 운영 배포 시 다음이 모두 보장된다:</p>
 * <ul>
 *   <li>Flyway 가 소유한 V3 스키마 위에서 데모 시더의 INSERT 가 제약(길이/NOT NULL/CHECK/unique) 위반 없이 적재됨</li>
 *   <li>exam_type CHECK 가 {MIDTERM, FINAL} 만 허용하도록 정리된 뒤에도 시험 적재가 성공함</li>
 *   <li>시딩 직후 분석(OLAP) ETL 이 정상 적재됨</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles({"test", "prod"})
@TestPropertySource(properties = "app.demo.enabled=true") // test 프로필의 false 를 강제로 덮어씀
@Testcontainers
class DemoSeedProdTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private JdbcTemplate jdbc;

    @Test
    @DisplayName("prod: 데모 시더가 Flyway 스키마 위에서 정상 적재 + 분석 ETL 동작")
    void demoSeedRunsUnderProd() {
        // 컨텍스트 기동 자체가 validate 통과 + DemoDataInitializer(시더+ETL) 실행 완료의 증거.

        // 1) 계정: admin 1 + teacher 6 + student 25 + parent 25 + PENDING 3 = 60
        assertThat(count("SELECT COUNT(*) FROM users")).isEqualTo(60);
        assertThat(count("SELECT COUNT(*) FROM users WHERE login_id = 'teacher01' AND role = 'TEACHER'")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM users WHERE role = 'STUDENT' AND status = 'ACTIVE'")).isEqualTo(25);
        assertThat(count("SELECT COUNT(*) FROM users WHERE role = 'PARENT'")).isEqualTo(25);
        assertThat(count("SELECT COUNT(*) FROM users WHERE role = 'STUDENT' AND status = 'PENDING'")).isEqualTo(3);

        // 2) 도메인 데이터: 과목6 · 반5 · 과목배정30 · 시험2 · 성적 25*6*2=300
        assertThat(count("SELECT COUNT(*) FROM subjects")).isEqualTo(6);
        assertThat(count("SELECT COUNT(*) FROM classrooms")).isEqualTo(5);
        assertThat(count("SELECT COUNT(*) FROM subject_assignments")).isEqualTo(30);
        assertThat(count("SELECT COUNT(*) FROM exams")).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM student_grades")).isEqualTo(300);

        // 3) exam_type CHECK({MIDTERM, FINAL}) 위반 없이 적재됨
        assertThat(count("SELECT COUNT(*) FROM exams WHERE exam_type IN ('MIDTERM','FINAL')")).isEqualTo(2);

        // 4) 부가 도메인이 비어있지 않음(전 테이블 시딩 확인 표본)
        assertThat(count("SELECT COUNT(*) FROM attendances")).isGreaterThan(0);
        assertThat(count("SELECT COUNT(*) FROM feedbacks WHERE status = 'PUBLISHED'")).isGreaterThan(0);
        assertThat(count("SELECT COUNT(*) FROM submissions")).isGreaterThan(0);
        assertThat(count("SELECT COUNT(*) FROM parent_invitations")).isEqualTo(3);
        assertThat(count("SELECT COUNT(*) FROM notifications")).isGreaterThan(0);

        // 5) 분석(OLAP) ETL 이 시딩된 데이터로 적재됨
        assertThat(count("SELECT COUNT(*) FROM analytics.student_subject_summary")).isGreaterThan(0);
        assertThat(count("SELECT COUNT(*) FROM analytics.etl_run_log WHERE status = 'SUCCESS'")).isGreaterThanOrEqualTo(1);
    }

    private int count(String sql) {
        Integer n = jdbc.queryForObject(sql, Integer.class);
        return n == null ? 0 : n;
    }
}
