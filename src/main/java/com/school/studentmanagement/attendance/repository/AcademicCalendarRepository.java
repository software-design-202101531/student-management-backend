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

    Optional<AcademicCalendar> findByDate(LocalDate date);
}
