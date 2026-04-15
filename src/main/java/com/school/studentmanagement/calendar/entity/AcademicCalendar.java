package com.school.studentmanagement.calendar.entity;

import com.school.studentmanagement.global.enums.DayType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "academic_calendars")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AcademicCalendar {
    // 학사 일정 등록 엔티티

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayType dayType; // HOLIDAY(공휴일), VACATION(방학), EVENT(행사)

    @Column(nullable = false)
    private String description; // 개교기념일, 추석 등 설명

    @Builder
    public AcademicCalendar(LocalDate date, DayType dayType, String description) {
        this.date = date;
        this.dayType = dayType;
        this.description = description;
    }
}
