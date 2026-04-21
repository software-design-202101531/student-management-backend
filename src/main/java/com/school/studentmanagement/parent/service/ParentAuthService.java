package com.school.studentmanagement.parent.service;

import com.school.studentmanagement.global.enums.Gender;
import com.school.studentmanagement.global.enums.RelationType;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.enums.UserStatus;
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
        ParentInvitation invitation = invitationRepository.findValidInvitation(
                request.getYear(),
                request.getGrade(),
                request.getClassNum(),
                request.getStudentNum(),
                request.getStudentName(),
                request.getParentPhone()
        ).orElseThrow(() -> new BusinessException(ErrorCode.PARENT_VERIFY_FAILED));

        return invitation.getId();
    }

    @Transactional
    public void registerParent(ParentRegisterRequest request) {
        ParentInvitation invitation = invitationRepository.findById(request.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITATION_NOT_FOUND));

        User user = User.builder()
                .loginId(request.getLoginId())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .status(UserStatus.ACTIVE)
                .gender(getGender(invitation))
                .role(UserRole.PARENT)
                .build();
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

    private Gender getGender(ParentInvitation invitation) {
        if (invitation.getRelationType() == RelationType.FATHER) return Gender.MALE;
        if (invitation.getRelationType() == RelationType.MOTHER) return Gender.FEMALE;
        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                "학생과의 관계 정의에 문제 발생, 관리자에게 문의 주세요");
    }
}
