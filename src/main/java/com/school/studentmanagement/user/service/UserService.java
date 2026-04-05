package com.school.studentmanagement.user.service;

import com.school.studentmanagement.affiliation.repository.StudentAffiliationRepository;
import com.school.studentmanagement.global.enums.UserStatus;
import com.school.studentmanagement.user.dto.StudentActivationRequest;
import com.school.studentmanagement.user.dto.VerifyStudentRequest;
import com.school.studentmanagement.user.entity.Student;
import com.school.studentmanagement.user.entity.User;
import com.school.studentmanagement.user.repository.StudentRepository;
import com.school.studentmanagement.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final StudentAffiliationRepository affiliationRepository;
    private final PasswordEncoder passwordEncoder;

    // 학생 정보 검증 메서드
    @Transactional(readOnly = true)
    public Long verifyStudent(VerifyStudentRequest request) {
        User pendingUser = affiliationRepository.findPendingStudentUser(
                request.getAcademicYear(),
                request.getGrade(),
                request.getClassNum(),
                request.getStudentNum(),
                request.getName()
        ).orElseThrow(() -> new IllegalArgumentException("입력하신 정보와 일치하는 가입 대기 정보가 없습니다"));

        // 일치하면 계정 활성화를 위해 해당 유저의 id를 프론트에 넘긴다
        return pendingUser.getId();
    }

    // 계정 활성화 메서드
    @Transactional
    public void activateStudentAccount(StudentActivationRequest request) {

        // id를 이용하여 유저를 찾기
        User user = userRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사용자입니다"));

        Student student = studentRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 학생 정보입니다"));

        // 활성화 안 된 유저인지 확인하기
        if(user.getStatus() != UserStatus.PENDING){
            throw new IllegalArgumentException("이미 활성화 된 계정입니다");
        }

        // 사용자 입력 정보 업데이트(아이디, 비밀번호)
        user.activateAccount(
                request.getLoginId(),
                passwordEncoder.encode(request.getPassword())
        );

        // Student 세부 정보 업데이트
        student.activateStudentInfo(
                request.getAddress(),
                request.getPhoneNumber()
        );
    }
}
