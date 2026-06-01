package com.school.studentmanagement.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class AppConfig {

    // 학사 일정(학년도/학기/마감) 계산 기준 시계 — 한국 표준시 고정.
    // Clock 주입으로 AcademicCalendarUtil의 시간 의존 로직을 테스트 가능하게 한다.
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
