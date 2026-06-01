package com.school.studentmanagement.parent.service;

import com.school.studentmanagement.global.enums.Gender;
import com.school.studentmanagement.global.enums.RelationType;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.parent.dto.ParentRegisterRequest;
import com.school.studentmanagement.parent.dto.VerifyParentRequest;
import com.school.studentmanagement.parent.entity.Parent;
import com.school.studentmanagement.parent.entity.ParentInvitation;
import com.school.studentmanagement.parent.entity.ParentStudentMapping;
import com.school.studentmanagement.parent.repository.ParentInvitationRepository;
import com.school.studentmanagement.parent.repository.ParentRepository;
import com.school.studentmanagement.parent.repository.ParentStudentMappingRepository;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.student.repository.StudentRepository;
import com.school.studentmanagement.user.entity.User;
import com.school.studentmanagement.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ParentAuthService {

    private final ParentInvitationRepository invitationRepository;
    private final ParentRepository parentRepository;
    private final ParentStudentMappingRepository mappingRepository;
    private final PasswordEncoder passwordEncoder;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Long verifyParent(VerifyParentRequest request) {
        ParentInvitation invitation = findValidInvitation(
                request.getYear(),
                request.getGrade(),
                request.getClassNum(),
                request.getStudentNum(),
                request.getStudentName(),
                request.getParentPhone()
        );

        return invitation.getId();
    }

    @Transactional
    public void registerParent(ParentRegisterRequest request) {
        // 클라이언트가 보낸 PK를 신뢰하지 않고, 신원정보로 유효 초대장을 직접 재조회한다.
        // (임의 초대장 PK로 타인의 자녀에 연결하던 계정 탈취 경로를 차단)
        ParentInvitation invitation = findValidInvitation(
                request.getYear(),
                request.getGrade(),
                request.getClassNum(),
                request.getStudentNum(),
                request.getStudentName(),
                request.getParentPhone()
        );

        // 아이디 중복 시 DB 제약 위반(500) 대신 친화적인 409 응답
        if (userRepository.findByLoginId(request.getLoginId()).isPresent()) {
            throw new BusinessException(ErrorCode.LOGIN_ID_DUPLICATED);
        }

        User user = User.createActive(
                request.getLoginId(),
                passwordEncoder.encode(request.getPassword()),
                request.getName(),
                getGender(invitation),
                UserRole.PARENT
        );
        userRepository.save(user);

        Parent parent = Parent.builder()
                .user(user)
                .phoneNumber(invitation.getPhoneNumber())
                .relationType(invitation.getRelationType())
                .build();
        parentRepository.save(parent);

        Student student = studentRepository.findById(invitation.getStudent().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));

        ParentStudentMapping mapping = new ParentStudentMapping(parent, student);
        mappingRepository.save(mapping);

        invitationRepository.delete(invitation);
    }

    // 신원정보로 유효(미만료) 초대장을 조회한다. verify/register가 공유한다.
    private ParentInvitation findValidInvitation(Integer year, Integer grade, Integer classNum,
                                                 Integer studentNum, String studentName, String parentPhone) {
        LocalDateTime validFrom = LocalDateTime.now().minusDays(ParentInvitation.INVITATION_VALID_DAYS);
        return invitationRepository.findValidInvitation(
                year, grade, classNum, studentNum, studentName, parentPhone, validFrom
        ).orElseThrow(() -> new BusinessException(ErrorCode.PARENT_VERIFY_FAILED));
    }

    private Gender getGender(ParentInvitation invitation) {
        if (invitation.getRelationType() == RelationType.FATHER) return Gender.MALE;
        if (invitation.getRelationType() == RelationType.MOTHER) return Gender.FEMALE;
        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                "학생과의 관계 정의에 문제 발생, 관리자에게 문의 주세요");
    }
}
