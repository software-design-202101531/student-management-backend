package com.school.studentmanagement.grade.dto;

import com.school.studentmanagement.global.enums.SemesterClosureMethod;
import com.school.studentmanagement.grade.entity.SemesterClosure;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SemesterClosureResponse {

    private Integer academicYear;
    private Integer semester;
    private boolean closed;

    private LocalDateTime closedAt;
    private SemesterClosureMethod method;
    private Long closedByUserId;
    private String closedByName;
    private String reason;
    private Integer filledCount;

    public static SemesterClosureResponse closed(SemesterClosure closure) {
        return SemesterClosureResponse.builder()
                .academicYear(closure.getAcademicYear())
                .semester(closure.getSemester())
                .closed(true)
                .closedAt(closure.getClosedAt())
                .method(closure.getMethod())
                .closedByUserId(closure.getClosedByUserId())
                .closedByName(closure.getClosedByName())
                .reason(closure.getReason())
                .filledCount(closure.getFilledCount())
                .build();
    }

    public static SemesterClosureResponse open(int year, int semester) {
        return SemesterClosureResponse.builder()
                .academicYear(year)
                .semester(semester)
                .closed(false)
                .build();
    }
}
