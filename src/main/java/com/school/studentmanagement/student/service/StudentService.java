package com.school.studentmanagement.student.service;

import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.global.enums.UserStatus;
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
        User user = userRepository.findById(request.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Student student = studentRepository.findById(request.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));

        if (user.getStatus() != UserStatus.PENDING) {
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_ACTIVE);
        }

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
