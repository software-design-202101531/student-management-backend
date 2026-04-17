package com.school.studentmanagement.attendance.repository;

import com.school.studentmanagement.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance,Long> {

    // 반 학생들의 ID 리스트를 통해 해당 날짜의 특이사항을 가져온다
    @Query("SELECT a FROM Attendance a JOIN FETCH a.student " +
            "WHERE a.student.id IN :studentIds AND a.date = :date")
    List<Attendance> findByStudentIdsAndDate(@Param("studentIds") List<Long> studentIds, @Param("date") LocalDate date);
}
