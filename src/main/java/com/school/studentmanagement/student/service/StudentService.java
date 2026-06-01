package com.school.studentmanagement.student.service;

import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.student.dto.StudentActivationRequest;
import com.school.studentmanagement.student.dto.VerifyStudentRequest;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.student.repository.StudentRepository;
import com.school.studentmanagement.user.entity.User;
import com.school.studentmanagement.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final StudentAffiliationRepository affiliationRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Long verifyStudent(VerifyStudentRequest request) {
        User pendingUser = affiliationRepository.findPendingStudentUser(
                request.getAcademicYear(),
                request.getGrade(),
                request.getClassNum(),
                request.getStudentNum(),
                request.getName()
        ).orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_VERIFY_FAILED));

        return pendingUser.getId();
    }

    @Transactional
    public void activateStudentAccount(StudentActivationRequest request) {
        // 클라이언트가 보낸 PK를 신뢰하지 않고, 신원정보로 가입대기 학생을 직접 찾아 활성화한다.
        // (PK만으로 임의 계정을 활성화하던 계정 탈취 경로를 차단)
        User user = affiliationRepository.findPendingStudentUser(
                request.getAcademicYear(),
                request.getGrade(),
                request.getClassNum(),
                request.getStudentNum(),
                request.getName()
        ).orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_VERIFY_FAILED));

        Student student = studentRepository.findById(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));

        // 아이디 중복 시 DB 제약 위반(500) 대신 친화적인 409 응답
        if (userRepository.findByLoginId(request.getLoginId()).isPresent()) {
            throw new BusinessException(ErrorCode.LOGIN_ID_DUPLICATED);
        }

        // 활성화 가능 여부(PENDING) 검증은 User.activateAccount 내부에서 수행한다
        user.activateAccount(
                request.getLoginId(),
                passwordEncoder.encode(request.getPassword())
        );

        student.activateStudentInfo(
                request.getAddress(),
                request.getPhoneNumber()
        );
    }
}
