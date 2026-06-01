package com.school.studentmanagement.student.service;

import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.global.storage.FileStorageService;
import com.school.studentmanagement.global.util.AcademicCalendarUtil;
import com.school.studentmanagement.student.dto.StudentContactUpdateRequest;
import com.school.studentmanagement.student.dto.StudentProfileResponse;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.teacher.entity.Teacher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 담임 교사가 자기 반 학생의 학생부 기본 프로필을 조회/수정한다.
 * 담임 여부는 현재 학기 소속 학급의 homeroomTeacher로 판정 (record/attendance/grade 도메인과 동일 규칙).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherStudentProfileService {

    private final StudentAffiliationRepository studentAffiliationRepository;
    private final FileStorageService fileStorageService;
    private final AcademicCalendarUtil academicCalendarUtil;

    public StudentProfileResponse getProfile(Long studentId, Long teacherId) {
        StudentAffiliation affiliation = loadCurrentAffiliationOrThrow(studentId);
        ensureHomeroomTeacher(affiliation, teacherId);
        return buildResponse(affiliation);
    }

    @Transactional
    public StudentProfileResponse updateContact(Long studentId, Long teacherId, StudentContactUpdateRequest request) {
        StudentAffiliation affiliation = loadCurrentAffiliationOrThrow(studentId);
        ensureHomeroomTeacher(affiliation, teacherId);
        affiliation.getStudent().updateContactInfo(request.getAddress(), request.getPhoneNumber());
        return buildResponse(affiliation);
    }

    private StudentAffiliation loadCurrentAffiliationOrThrow(Long studentId) {
        int year = academicCalendarUtil.getCurrentAcademicYear();
        int semester = academicCalendarUtil.getCurrentSemester();
        return studentAffiliationRepository.findWithAllDetails(studentId, year, semester)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_ASSIGNED));
    }

    private void ensureHomeroomTeacher(StudentAffiliation affiliation, Long teacherId) {
        Teacher homeroom = affiliation.getClassroom().getHomeroomTeacher();
        if (homeroom == null || !homeroom.getId().equals(teacherId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "담임 교사만 학생 프로필을 조회/수정할 수 있습니다");
        }
    }

    private StudentProfileResponse buildResponse(StudentAffiliation affiliation) {
        Student student = affiliation.getStudent();
        Teacher homeroom = affiliation.getClassroom().getHomeroomTeacher();
        String homeroomName = homeroom != null ? homeroom.getUser().getName() : null;
        String profileImageUrl = fileStorageService.presignedGetUrl(student.getProfileImageKey());

        return StudentProfileResponse.builder()
                .studentId(student.getId())
                .name(student.getUser().getName())
                .grade(affiliation.getClassroom().getGrade())
                .classNum(affiliation.getClassroom().getClassNum())
                .studentNum(affiliation.getStudentNum())
                .address(student.getAddress())
                .phoneNumber(student.getPhoneNumber())
                .profileImageUrl(profileImageUrl)
                .enrollmentYear(student.getEnrollmentYear())
                .homeroomTeacherName(homeroomName)
                .build();
    }
}
