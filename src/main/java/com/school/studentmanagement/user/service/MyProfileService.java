package com.school.studentmanagement.user.service;

import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.global.storage.FileStorageService;
import com.school.studentmanagement.global.util.AcademicCalendarUtil;
import com.school.studentmanagement.parent.entity.Parent;
import com.school.studentmanagement.parent.repository.ParentRepository;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.student.repository.StudentRepository;
import com.school.studentmanagement.teacher.entity.Teacher;
import com.school.studentmanagement.teacher.repository.TeacherRepository;
import com.school.studentmanagement.user.dto.MyProfileResponse;
import com.school.studentmanagement.user.entity.User;
import com.school.studentmanagement.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


// 로그인한 본인의 프로필 조회. 역할에 따라 해당 도메인에서 조회 후 공통+역할별 정보로 조립한다.
// 사진 key는 presigned URL로 변환(없으면 null)
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyProfileService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentAffiliationRepository studentAffiliationRepository;
    private final FileStorageService fileStorageService;
    private final AcademicCalendarUtil academicCalendarUtil;

    // 개인 정보 반환 메서드
    public MyProfileResponse getMyProfile(Long userId, UserRole role) {
        return switch (role) {
            case STUDENT -> buildStudentProfile(userId);
            case PARENT -> buildParentProfile(userId);
            case TEACHER -> buildTeacherProfile(userId);
            case ADMIN -> buildAdminProfile(userId);
        };
    }

    // 학생 정보 반환 메서드
    private MyProfileResponse buildStudentProfile(Long userId) {
        Student student = studentRepository.findByIdWithUserAndHomeroom(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));

        // 이미지 url
        String profileImageUrl = fileStorageService.presignedGetUrl(student.getProfileImageKey());

        // 현재 학년도/학기 배정 반 — 미배정이면 학년/반 null
        int year = academicCalendarUtil.getCurrentAcademicYear();
        int semester = academicCalendarUtil.getCurrentSemester();
        Optional<Classroom> classroom = studentAffiliationRepository
                .findWithAllDetails(userId, year, semester)
                .map(StudentAffiliation::getClassroom);

        String homeroomTeacherName = student.getHomeroomTeacher() != null
                ? student.getHomeroomTeacher().getUser().getName()
                : null;

        MyProfileResponse.StudentDetail detail = new MyProfileResponse.StudentDetail(
                student.getAddress(),
                student.getPhoneNumber(),
                student.getEnrollmentYear(),
                classroom.map(Classroom::getGrade).orElse(null),
                classroom.map(Classroom::getClassNum).orElse(null),
                homeroomTeacherName
        );
        return MyProfileResponse.ofStudent(student.getUser(), profileImageUrl, detail);
    }

    // 부모님 개인정보 반환 메서드
    private MyProfileResponse buildParentProfile(Long userId) {
        Parent parent = parentRepository.findByIdWithUser(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        MyProfileResponse.ParentDetail detail = new MyProfileResponse.ParentDetail(
                parent.getPhoneNumber(),
                parent.getRelationType()
        );
        return MyProfileResponse.ofParent(parent.getUser(), detail);
    }

    // 선생님 개인정보 반환 메서드
    private MyProfileResponse buildTeacherProfile(Long userId) {
        Teacher teacher = teacherRepository.findByIdWithDetails(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_NOT_FOUND));

        String profileImageUrl = fileStorageService.presignedGetUrl(teacher.getProfileImageKey());

        MyProfileResponse.TeacherDetail detail = new MyProfileResponse.TeacherDetail(
                teacher.getEmployeeNumber(),
                teacher.getSubject().getName(),
                teacher.getOfficeLocation(),
                teacher.getOfficePhoneNumber(),
                teacher.getEmploymentStatus()
        );
        return MyProfileResponse.ofTeacher(teacher.getUser(), profileImageUrl, detail);
    }

    // 관리자 개인정보 반환 메서드
    private MyProfileResponse buildAdminProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return MyProfileResponse.ofAdmin(user);
    }
}
