package com.school.studentmanagement.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * test 프로필 통합 테스트 공통 베이스.
 *
 * <p>단일 Postgres 컨테이너(싱글톤)를 전체 통합 테스트 클래스가 공유한다 — 클래스마다 컨테이너를
 * 띄우지 않아 기동 비용을 크게 줄인다. 컨테이너는 클래스 최초 로드 시 1회 start 되고, JVM 종료 시
 * Testcontainers(Ryuk)가 정리한다(명시적 stop 없음 → 싱글톤 패턴).
 *
 * <p>격리: 각 Spring 컨텍스트는 {@code ddl-auto=create-drop} 으로 public 스키마를 새로 만든다.
 * analytics 스키마는 Flyway(멱등), 행특 부분 유니크 인덱스는 {@code BehaviorRecordIndexInitializer}
 * (CREATE ... IF NOT EXISTS, 멱등)가 매 컨텍스트 기동 시 보장하므로 공유 DB에서도 안전하다.
 *
 * <p>주의: prod 프로필 테스트(Flyway 전체 소유 + {@code ddl-auto=validate})는 create-drop 과
 * 충돌하므로 이 베이스를 쓰지 않고 자체 컨테이너를 유지한다.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
