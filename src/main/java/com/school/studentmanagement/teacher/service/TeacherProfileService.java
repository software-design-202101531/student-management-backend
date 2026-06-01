package com.school.studentmanagement.teacher.service;

import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.classroom.repository.ClassRoomRepository;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.global.util.AcademicCalendarUtil;
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
    private final AcademicCalendarUtil academicCalendarUtil;

    public TeacherProfileResponse getMyProfile(Long userId) {
        Teacher teacher = teacherRepository.findByIdWithDetails(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_NOT_FOUND));

        // 현재 학년도/학기의 담임 반만 조회 (교사는 여러 학기 담임 이력을 가질 수 있어 단건 조건 필수)
        int currentYear = academicCalendarUtil.getCurrentAcademicYear();
        int currentSemester = academicCalendarUtil.getCurrentSemester();
        Optional<Classroom> homeroom = classRoomRepository
                .findClassroomByHomeroomTeacherIdAndAcademicYearAndSemester(
                        teacher.getId(), currentYear, currentSemester);

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
