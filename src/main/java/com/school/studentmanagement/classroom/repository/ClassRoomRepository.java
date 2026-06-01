package com.school.studentmanagement.classroom.repository;

import com.school.studentmanagement.classroom.entity.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClassRoomRepository extends JpaRepository<Classroom, Long> {

    // 특정 선생님이 해당 학년도/학기에 담임인 반 조회
    Optional<Classroom> findClassroomByHomeroomTeacherIdAndAcademicYearAndSemester(Long homeroomTeacherId, Integer academicYear, Integer semester);
}
