package com.school.studentmanagement.global.enums;

public enum ExamAttendanceStatus {
    PRESENT,        // 정상 응시 — rawScore가 점수
    ABSENT,         // 결시 — rawScore = null, 평균 산출에서 제외
    CHEATED,        // 부정행위 — rawScore = 0 강제, 평균에 포함
    NOT_SUBMITTED   // 학기 마감 시 자동 채움 — rawScore = 0, 평균에 포함
}
