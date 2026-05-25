package com.school.studentmanagement.grade.scheduler;

import com.school.studentmanagement.grade.service.SemesterClosureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "school.semester-closure.auto.enabled", havingValue = "true", matchIfMissing = true)
public class SemesterClosureScheduler {

    private final SemesterClosureService semesterClosureService;

    /**
     * 매일 새벽 3시 (Asia/Seoul) — AcademicCalendarUtil.isModifiable이 false인 학기 중
     * 아직 마감되지 않은 것들을 자동으로 AUTO_FALLBACK 마감.
     * <p>
     * `school.semester-closure.auto.enabled=false` 로 비활성화 가능.
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void autoCloseExpiredSemesters() {
        try {
            int closedCount = semesterClosureService.autoCloseExpired();
            if (closedCount > 0) {
                log.info("[SemesterClosureScheduler] {} 학기 자동 마감 완료", closedCount);
            }
        } catch (Exception e) {
            log.error("[SemesterClosureScheduler] 자동 마감 실패", e);
        }
    }
}
