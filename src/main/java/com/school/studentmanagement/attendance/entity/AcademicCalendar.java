package com.school.studentmanagement.attendance.entity;

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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayType dayType;

    @Column(nullable = false)
    private String description;

    @Builder
    public AcademicCalendar(LocalDate date, DayType dayType, String description) {
        this.date = date;
        this.dayType = dayType;
        this.description = description;
    }
}
