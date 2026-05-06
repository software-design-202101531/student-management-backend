package com.school.studentmanagement.student.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class StudentMyRecordResponse {

    private Integer academicYear;
    private Integer semester;
    private String behaviorRecord;
    private List<SubjectRecordDto> subjectRecords;

    @Getter
    @Builder
    public static class SubjectRecordDto {
        private String subjectName;
        private String content;
    }
}
