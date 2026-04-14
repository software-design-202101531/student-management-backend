package com.school.studentmanagement.record.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BehaviorRecordResponse {
    Long recordId;  // 기존 작성 내용이 없다면 null
    String content; // 내용 본문
    boolean canEdit;// 마감 여부
}
