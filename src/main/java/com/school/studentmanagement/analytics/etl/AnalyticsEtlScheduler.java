package com.school.studentmanagement.analytics.etl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 분석 배치 ETL 스케줄러. 기본 매일 03:30(KST). analytics.etl.cron 으로 조정.
 * (이벤트 기반 증분 적재 Phase 2와 함께 람다식으로 운영 — 배치가 전량 보정 베이스라인)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsEtlScheduler {

    private final AnalyticsEtlService analyticsEtlService;

    @Scheduled(cron = "${analytics.etl.cron:0 30 3 * * *}", zone = "Asia/Seoul")
    public void runDaily() {
        try {
            analyticsEtlService.runAll();
        } catch (Exception e) {
            log.error("[analytics-etl] 배치 적재 실패", e);
        }
    }
}
