package com.school.studentmanagement.parent.service;

import com.school.studentmanagement.global.enums.Gender;
import com.school.studentmanagement.global.enums.RelationType;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.enums.UserStatus;
import com.school.studentmanagement.global.exception.BusinessException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ParentAuthServiceTest {

    @InjectMocks private ParentAuthService parentAuthService;
    @Mock private ParentInvitationRepository invitationRepository;
    @Mock private ParentRepository parentRepository;
    @Mock private ParentStudentMappingRepository mappingRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private StudentRepository studentRepository;
    @Mock private UserRepository userRepository;

    private Student student;
    private ParentInvitation fatherInvitation;
    private ParentInvitation motherInvitation;

    private static final Long INVITATION_ID = 10L;
    private static final Long STUDENT_ID    = 1L;

    @BeforeEach
    void setUp() {
        User studentUser = User.builder().id(STUDENT_ID).name("홍길동")
                .gender(Gender.MALE).role(UserRole.STUDENT).status(UserStatus.ACTIVE).build();
        student = Student.builder().id(STUDENT_ID).user(studentUser).enrollmentYear(2026).build();

        fatherInvitation = ParentInvitation.builder()
                .student(student).phoneNumber("01011112222").relationType(RelationType.FATHER).build();
        ReflectionTestUtils.setField(fatherInvitation, "id", INVITATION_ID);

        motherInvitation = ParentInvitation.builder()
                .student(student).phoneNumber("01033334444").relationType(RelationType.MOTHER).build();
        ReflectionTestUtils.setField(motherInvitation, "id", INVITATION_ID + 1);
    }

    @Nested
    @DisplayName("학부모 자격 검증 (verifyParent)")
    class VerifyParentTest {

        @Test
        @DisplayName("성공: 유효한 초대 정보로 invitationId 반환")
        void verifyParent_Success() {
            given(invitationRepository.findValidInvitation(any(), any(), any(), any(), any(), any()))
                    .willReturn(Optional.of(fatherInvitation));

            Long result = parentAuthService.verifyParent(new VerifyParentRequest());

            assertThat(result).isEqualTo(INVITATION_ID);
        }

        @Test
        @DisplayName("실패: 일치하는 초대 정보가 없으면 BusinessException")
        void verifyParent_Fail_InvalidInfo() {
            given(invitationRepository.findValidInvitation(any(), any(), any(), any(), any(), any()))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> parentAuthService.verifyParent(new VerifyParentRequest()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("입력하신 정보가 올바르지 않습니다");
        }
    }

    @Nested
    @DisplayName("학부모 회원가입 (registerParent)")
    class RegisterParentTest {

        @Test
        @DisplayName("성공(FATHER): 부 관계 초대장으로 MALE 계정 생성, 매핑 저장, 초대장 삭제")
        void registerParent_Success_Father() {
            ParentRegisterRequest request = buildRegisterRequest(INVITATION_ID, "parent01", "pass1234!", "홍아버지");
            User savedUser = User.builder().id(99L).loginId("parent01").name("홍아버지")
                    .gender(Gender.MALE).role(UserRole.PARENT).status(UserStatus.ACTIVE).build();

            given(invitationRepository.findById(INVITATION_ID)).willReturn(Optional.of(fatherInvitation));
            given(passwordEncoder.encode("pass1234!")).willReturn("encoded");
            given(userRepository.save(any())).willReturn(savedUser);
            given(parentRepository.save(any())).willReturn(any());
            given(studentRepository.findById(STUDENT_ID)).willReturn(Optional.of(student));

            parentAuthService.registerParent(request);

            verify(userRepository).save(any(User.class));
            verify(parentRepository).save(any(Parent.class));
            verify(mappingRepository).save(any(ParentStudentMapping.class));
            verify(invitationRepository).delete(fatherInvitation);
        }

        @Test
        @DisplayName("성공(MOTHER): 모 관계 초대장으로 FEMALE 계정 생성")
        void registerParent_Success_Mother() {
            ParentRegisterRequest request = buildRegisterRequest(INVITATION_ID + 1, "parent02", "pass1234!", "홍어머니");
            User savedUser = User.builder().id(100L).loginId("parent02").name("홍어머니")
                    .gender(Gender.FEMALE).role(UserRole.PARENT).status(UserStatus.ACTIVE).build();

            given(invitationRepository.findById(INVITATION_ID + 1)).willReturn(Optional.of(motherInvitation));
            given(passwordEncoder.encode(any())).willReturn("encoded");
            given(userRepository.save(any())).willReturn(savedUser);
            given(studentRepository.findById(STUDENT_ID)).willReturn(Optional.of(student));

            parentAuthService.registerParent(request);

            verify(invitationRepository).delete(motherInvitation);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 초대장 ID → BusinessException")
        void registerParent_Fail_InvitationNotFound() {
            given(invitationRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> parentAuthService.registerParent(
                    buildRegisterRequest(999L, "id", "pw", "이름")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("유효하지 않은 초대 정보입니다");
        }
    }

    private ParentRegisterRequest buildRegisterRequest(Long id, String loginId, String password, String name) {
        ParentRegisterRequest req = new ParentRegisterRequest();
        ReflectionTestUtils.setField(req, "id", id);
        ReflectionTestUtils.setField(req, "loginId", loginId);
        ReflectionTestUtils.setField(req, "password", password);
        ReflectionTestUtils.setField(req, "name", name);
        return req;
    }
}
