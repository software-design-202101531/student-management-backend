package com.school.studentmanagement.user.service;

import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.classroom.repository.ClassRoomRepository;
import com.school.studentmanagement.user.dto.TeacherProfileResponse;
import com.school.studentmanagement.user.entity.Teacher;
import com.school.studentmanagement.user.repository.TeacherRepository;
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

        // 선생님 정보 가져오기
        Teacher teacher = teacherRepository.findByIdwithDetails(userId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));

        // 담임을 맡은 반이 있는지 확인
        Optional<Classroom> homeroom = classRoomRepository.findClassroomByHomeroomTeacher(teacher);

        // 리턴
        return TeacherProfileResponse.builder()
                .name(teacher.getUser().getName())
                .employeeNumber(teacher.getEmployeeNumber())
                .subjectName(teacher.getSubject().getName())
                .employmentStatus(teacher.getEmploymentStatus())
                .isHomeRoom(homeroom.isPresent())
                .homeroomClassId(homeroom.map(Classroom::getId).orElse(null))
                .grade(homeroom.map(Classroom::getGrade).orElse(null))
                .classNum(homeroom.map(Classroom::getClassNum).orElse(null))
                .build();
    }
}
