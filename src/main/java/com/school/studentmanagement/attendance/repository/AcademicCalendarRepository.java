package com.school.studentmanagement.attendance.repository;

import com.school.studentmanagement.attendance.entity.AcademicCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AcademicCalendarRepository extends JpaRepository<AcademicCalendar, Long> {

    @Query("SELECT ac FROM AcademicCalendar ac " +
            "WHERE YEAR(ac.date) = :year AND MONTH(ac.date) = :month " +
            "AND ac.dayType != 'WEEKDAY'")
    List<AcademicCalendar> findHolidaysByYearAndMonth(@Param("year") int year, @Param("month") int month);

    // 기간 단위 휴일 조회 — 학생 단위 출결 조회에서 사용
    @Query("SELECT ac FROM AcademicCalendar ac " +
            "WHERE ac.date BETWEEN :from AND :to " +
            "AND ac.dayType != 'WEEKDAY' " +
            "ORDER BY ac.date ASC")
    List<AcademicCalendar> findHolidaysByDateBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    Optional<AcademicCalendar> findByDate(LocalDate date);
}
