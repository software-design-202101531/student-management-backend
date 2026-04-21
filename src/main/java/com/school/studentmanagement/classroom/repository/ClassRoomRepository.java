package com.school.studentmanagement.classroom.repository;

import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.teacher.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClassRoomRepository extends JpaRepository<Classroom,Long> {

    // 특정 선생님이 담임인 반 정보 가져오기
    Optional<Classroom> findClassroomByHomeroomTeacher(Teacher teacher);

    Optional<Classroom> findClassroomByHomeroomTeacherIdAndAcademicYearAndSemester(Long homeroomTeacherId, Integer academicYear, Integer semester);
}
