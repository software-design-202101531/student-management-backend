package com.school.studentmanagement.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RecordCategory {
    SUBJECT_OPINION("교과 세부능력 및 특기사항"),
    BEHAVIOR_OPINION("행동특성 및 종합의견");

    private final String description;
}
