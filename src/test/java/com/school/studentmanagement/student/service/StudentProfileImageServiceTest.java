package com.school.studentmanagement.student.service;

import com.school.studentmanagement.global.dto.ProfileImageResponse;
import com.school.studentmanagement.global.enums.EmploymentStatus;
import com.school.studentmanagement.global.enums.Gender;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.enums.UserStatus;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.global.storage.ProfileImageManager;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.student.repository.StudentRepository;
import com.school.studentmanagement.teacher.entity.Teacher;
import com.school.studentmanagement.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StudentProfileImageServiceTest {

    @InjectMocks private StudentProfileImageService service;
    @Mock private StudentRepository studentRepository;
    @Mock private ProfileImageManager profileImageManager;

    private static final Long HOMEROOM_TEACHER_ID = 100L;
    private static final Long OTHER_TEACHER_ID = 999L;
    private static final Long STUDENT_ID = 1L;

    private final MultipartFile file =
            new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});

    private Student studentWithHomeroom(Long homeroomTeacherId) {
        User teacherUser = User.builder().id(homeroomTeacherId).name("담임").gender(Gender.MALE)
                .role(UserRole.TEACHER).status(UserStatus.ACTIVE).build();
        Teacher teacher = Teacher.builder().user(teacherUser).employeeNumber("EMP001")
                .officeLocation("본관").officePhoneNumber("02-000").employmentStatus(EmploymentStatus.ACTIVE).build();
        ReflectionTestUtils.setField(teacher, "id", homeroomTeacherId);

        User studentUser = User.builder().id(STUDENT_ID).name("학생").gender(Gender.MALE)
                .role(UserRole.STUDENT).status(UserStatus.ACTIVE).build();
        return Student.builder().id(STUDENT_ID).user(studentUser).homeroomTeacher(teacher).enrollmentYear(2026).build();
    }

    @Test
    @DisplayName("실패: 학생이 없으면 STUDENT_NOT_FOUND")
    void studentNotFound() {
        given(studentRepository.findById(STUDENT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateProfileImage(STUDENT_ID, HOMEROOM_TEACHER_ID, file))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDENT_NOT_FOUND);
        verify(profileImageManager, never()).replace(any(), any(), any(), any());
    }

    @Test
    @DisplayName("실패: 담임이 아닌 교사 → ACCESS_DENIED, 업로드 미수행")
    void notHomeroomTeacher() {
        given(studentRepository.findById(STUDENT_ID)).willReturn(Optional.of(studentWithHomeroom(HOMEROOM_TEACHER_ID)));

        assertThatThrownBy(() -> service.updateProfileImage(STUDENT_ID, OTHER_TEACHER_ID, file))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
        verify(profileImageManager, never()).replace(any(), any(), any(), any());
    }

    @Test
    @DisplayName("성공: 담임 교사면 매니저에 위임하고 presigned URL 반환")
    void homeroomTeacherSucceeds() {
        given(studentRepository.findById(STUDENT_ID)).willReturn(Optional.of(studentWithHomeroom(HOMEROOM_TEACHER_ID)));
        ProfileImageResponse expected = new ProfileImageResponse("https://minio/presigned");
        given(profileImageManager.replace(any(), any(), any(), any())).willReturn(expected);

        ProfileImageResponse result = service.updateProfileImage(STUDENT_ID, HOMEROOM_TEACHER_ID, file);

        assertThat(result).isEqualTo(expected);
        verify(profileImageManager).replace(any(), any(), any(), any());
    }
}
