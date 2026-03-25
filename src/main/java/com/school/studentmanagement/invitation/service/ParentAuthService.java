package com.school.studentmanagement.invitation.service;

import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.enums.UserStatus;
import com.school.studentmanagement.invitation.dto.ParentRegisterRequest;
import com.school.studentmanagement.invitation.dto.ParentVerifyRequest;
import com.school.studentmanagement.invitation.entity.ParentInvitation;
import com.school.studentmanagement.invitation.repository.ParentInvitationRepository;
import com.school.studentmanagement.user.entity.Parent;
import com.school.studentmanagement.user.entity.ParentStudentMapping;
import com.school.studentmanagement.user.entity.Student;
import com.school.studentmanagement.user.entity.User;
import com.school.studentmanagement.user.repository.ParentRepository;
import com.school.studentmanagement.user.repository.ParentStudentMappingRepository;
import com.school.studentmanagement.user.repository.StudentRepository;
import com.school.studentmanagement.user.repository.UserRepository;
import com.school.studentmanagement.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ParentAuthService {

    private final ParentInvitationRepository invitationRepository;
    private final ParentRepository parentRepository;
    private final ParentStudentMappingRepository mappingRepository;
    private final PasswordEncoder passwordEncoder;
    private final StudentRepository studentRepository;



    // 학부모 가입 자격 검증
    @Transactional(readOnly = true)
    public Long verifyParent(ParentVerifyRequest request) {
        // 데이터 가져오기
        ParentInvitation invitation = invitationRepository.findValidInvitation(
                request.getYear(),
                request.getGrade(),
                request.getClassNum(),
                request.getStudentNum(),
                request.getStudentName(),
                request.getParentPhone()
        ).orElseThrow(() -> new IllegalArgumentException("입력하신 정보가 올바르지 않습니다"));

        // 인증 성공 시 ID를 반환
        return invitation.getId();
    }

    // 최종 회원가입 및 데이터 전이
    @Transactional
    public void registerParent(ParentRegisterRequest request) {
        // id 겁증
        ParentInvitation invitation = invitationRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("이미 가입한 사용자입니다"));

        // 계정 생성
        Parent parent = Parent.createParentIdentity(
                request.getLoginId(),
                passwordEncoder.encode(request.getPassword()),
                request.getName(),
                invitation.getPhoneNumber(),
                invitation.getRelationType()
        );

        // 저장
        parentRepository.save(parent);

        // 자녀 매핑 및 인증 정보 삭제
        Student student = studentRepository.findById(invitation.getStudent().getId())
                .orElseThrow(() -> new IllegalArgumentException("자녀 정보를 찾을 수 없습니다"));


        // 자녀-학부모 매핑 테이블 연결
        ParentStudentMapping mapping = new ParentStudentMapping(parent, student);
        mappingRepository.save(mapping);

        // 사용을 마친 인증 정보 삭제
        invitationRepository.delete(invitation);
    }

}
