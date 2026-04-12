package com.school.studentmanagement.classroom.repository;

import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.user.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClassRoomRepository extends JpaRepository<Classroom,Long> {
    Optional<Classroom> findClassroomByHomeroomTeacher(Teacher teacher);
}
