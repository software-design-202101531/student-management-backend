package com.school.studentmanagement.calendar.repository;

import com.school.studentmanagement.calendar.entity.AcademicCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AcademicCalendarRepository extends JpaRepository<AcademicCalendar,Long> {

    // 달력 렌더링용: 특정 연/월의 휴일 정보만 싹 긁어오기
    @Query("SELECT ac FROM AcademicCalendar ac " +
            "WHERE YEAR(ac.date) = :year AND MONTH(ac.date) = :month " +
            "AND ac.dayType != 'WEEKDAY' ")
    List<AcademicCalendar> findHolidaysByYearAndMonth(@Param("year") int year, @Param("month") int month);

    // 일간 조회/저장용: 특정 날짜가 쉬는 날인지 단건 조회
    Optional<AcademicCalendar> findByDate(LocalDate date);
}
