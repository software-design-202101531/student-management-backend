package com.school.studentmanagement.global.config;

import com.school.studentmanagement.analytics.etl.AnalyticsEtlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 데모 데이터 시딩 진입점 — dev/prod 공용(기존 InitDataConfig를 대체).
 *
 * <p>활성 조건: {@code app.demo.enabled=true} 일 때만 동작한다. (프로필 무관)
 * 개발은 {@code application-dev.yml} 에서 기본 true, 운영은 {@code DEMO_SEED_ENABLED} 환경변수로 제어(기본 false).</p>
 *
 * <p>흐름: {@link DemoDataSeeder#seedIfNeeded()} 로 운영 데이터를 적재(트랜잭션 커밋)한 뒤,
 * 커밋된 데이터를 기반으로 {@link AnalyticsEtlService#runAll()} 를 호출해 분석(OLAP) 테이블을 즉시 채운다.</p>
 */
@Component
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DemoDataInitializer implements ApplicationRunner {

    private final DemoDataSeeder seeder;
    private final AnalyticsEtlService analyticsEtlService;

    @Override
    public void run(ApplicationArguments args) {
        boolean seeded = seeder.seedIfNeeded();
        if (!seeded) {
            return;
        }
        try {
            int rows = analyticsEtlService.runAll();
            log.info("[demo-seed] 분석 ETL 적재 완료 — upserted rows={}", rows);
        } catch (Exception e) {
            log.error("[demo-seed] 분석 ETL 적재 실패 (운영 데이터 시딩은 완료됨). /api/analytics/etl/run 으로 재시도 가능.", e);
        }
    }
}
