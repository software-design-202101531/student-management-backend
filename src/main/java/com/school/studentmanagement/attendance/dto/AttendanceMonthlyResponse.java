package com.school.studentmanagement.attendance.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class AttendanceMonthlyResponse {
    // 특정 달의 휴일 리스트 dto
    private int year;
    private int month;
    private List<HolidayDto> holidays; // 해당 달의 휴일 리스트

    @Getter
    @Builder
    public static class HolidayDto {
        private LocalDate date;
        private String name;    // ex) 개교기념일
    }
}
