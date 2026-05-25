package com.school.studentmanagement.consultation.service;

import com.school.studentmanagement.consultation.dto.ConsultationCreateRequest;
import com.school.studentmanagement.consultation.dto.ConsultationResponse;
import com.school.studentmanagement.consultation.entity.Consultation;
import com.school.studentmanagement.consultation.repository.ConsultationRepository;
import com.school.studentmanagement.global.enums.ConsultationVisibility;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.student.repository.StudentRepository;
import com.school.studentmanagement.teacher.entity.Teacher;
import com.school.studentmanagement.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    // 상담 내역 생성 (교사 전용) — visibility 미지정 시 RESTRICTED
    @Transactional
    public ConsultationResponse createConsultation(Long teacherId, ConsultationCreateRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_NOT_FOUND));

        Consultation consultation = Consultation.create(
                teacher, student, request.getConsultationDate(),
                request.getContent(), request.getNextPlan(), request.getVisibility());
        consultationRepository.save(consultation);

        return ConsultationResponse.from(consultation);
    }

    // 특정 학생 상담 내역 조회 — 권한 통과 조건(OR)에 맞는 데이터만 반환
    public List<ConsultationResponse> getStudentConsultations(Long studentId, Long requesterId, UserRole role) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));

        boolean isAdmin = role == UserRole.ADMIN;
        // 담임 여부는 학생당 1회만 평가 (모든 상담에 동일하게 적용)
        boolean isHomeroom = student.isHomeroomTeacher(requesterId);

        return consultationRepository.findAllByStudentId(studentId).stream()
                .filter(c -> canView(c, requesterId, isAdmin, isHomeroom))
                .map(ConsultationResponse::from)
                .toList();
    }

    // 공개 범위 토글 (작성자 본인 또는 관리자만) — RESTRICTED <-> ALL_TEACHERS
    @Transactional
    public ConsultationResponse toggleVisibility(Long consultationId, Long requesterId, UserRole role) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONSULTATION_NOT_FOUND));

        boolean isAdmin = role == UserRole.ADMIN;
        if (!isAdmin && !consultation.isAuthor(requesterId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "작성자 본인 또는 관리자만 공개 범위를 변경할 수 있습니다");
        }

        consultation.toggleVisibility();
        return ConsultationResponse.from(consultation);
    }

    // 조회 권한 통과 조건 (하나라도 만족하면 true)
    //  1) 관리자
    //  2) 공개 범위가 ALL_TEACHERS
    //  3) 작성자 본인
    //  4) 해당 학생의 담임 교사
    private boolean canView(Consultation consultation, Long requesterId, boolean isAdmin, boolean isHomeroom) {
        return isAdmin
                || consultation.getVisibility() == ConsultationVisibility.ALL_TEACHERS
                || consultation.isAuthor(requesterId)
                || isHomeroom;
    }
}
