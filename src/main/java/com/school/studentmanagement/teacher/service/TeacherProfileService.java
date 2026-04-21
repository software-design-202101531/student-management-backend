package com.school.studentmanagement.teacher.service;

import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.classroom.repository.ClassRoomRepository;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.teacher.dto.TeacherProfileResponse;
import com.school.studentmanagement.teacher.entity.Teacher;
import com.school.studentmanagement.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherProfileService {

    private final TeacherRepository teacherRepository;
    private final ClassRoomRepository classRoomRepository;

    public TeacherProfileResponse getMyProfile(Long userId) {
        Teacher teacher = teacherRepository.findByIdwithDetails(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_NOT_FOUND));

        Optional<Classroom> homeroom = classRoomRepository.findClassroomByHomeroomTeacher(teacher);

        return TeacherProfileResponse.builder()
                .name(teacher.getUser().getName())
                .employeeNumber(teacher.getEmployeeNumber())
                .subjectName(teacher.getSubject().getName())
                .subjectId(teacher.getSubject().getId())
                .employmentStatus(teacher.getEmploymentStatus())
                .isHomeRoom(homeroom.isPresent())
                .homeroomClassId(homeroom.map(Classroom::getId).orElse(null))
                .grade(homeroom.map(Classroom::getGrade).orElse(null))
                .classNum(homeroom.map(Classroom::getClassNum).orElse(null))
                .build();
    }
}
