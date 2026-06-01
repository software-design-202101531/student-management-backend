package com.school.studentmanagement.prod;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 운영 전환 검증: prod 프로필에서 Flyway 가 전체 스키마를 적용하고
 * Hibernate ddl-auto:validate 가 그 스키마에 대해 통과하는지 확인한다.
 *
 * - 프로필 {test, prod}: test 는 ${ENV} placeholder + auto-startup=false 제공,
 *   prod 가 ddl-auto:validate + Flyway 2개 위치(db/migration, db/prod-migration)로 덮어쓴다.
 *   (prod 가 뒤에 활성화되므로 충돌 키는 prod 값이 우선)
 * - 빈 Testcontainers DB → Flyway 가 V1/V2(analytics) + V3(운영 public baseline) 적용
 *   → Hibernate validate 가 24개 @Entity 매핑을 검증.
 * - 컨텍스트가 기동되면(=validate 통과) 운영 스키마 관리가 정합함이 증명된다.
 *   prod 에서는 데모 시딩이 기본 비활성(app.demo.enabled=false)이라 시드도 없다.
 */
@SpringBootTest
@ActiveProfiles({"test", "prod"})
@Testcontainers
class ProdSchemaValidationTest {

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
    @DisplayName("prod: Flyway 전체 스키마 적용 + ddl-auto:validate 통과(컨텍스트 기동)")
    void flywayOwnsSchemaAndValidatePasses() {
        // 컨텍스트가 떴다는 것 자체가 validate 통과의 증거. 추가로 산출물을 직접 확인한다.

        // 1) Flyway 가 운영 baseline(V3) 까지 적용했는가
        Integer v3 = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '3' AND success = true",
                Integer.class);
        assertThat(v3).isEqualTo(1);

        // 2) 운영 public 테이블이 Flyway 로 생성되었는가(엔티티 대표 테이블 표본)
        Integer publicTables = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = 'public' AND table_type = 'BASE TABLE' " +
                        "AND table_name IN ('users','students','assignments','submissions','student_records')",
                Integer.class);
        assertThat(publicTables).isEqualTo(5);

        // 3) analytics 스키마도 공존하는가 (요약 4종 + etl_run_log + classroom_exam_subject_stats = 6)
        Integer analyticsTables = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'analytics'",
                Integer.class);
        assertThat(analyticsTables).isEqualTo(6);
    }
}
