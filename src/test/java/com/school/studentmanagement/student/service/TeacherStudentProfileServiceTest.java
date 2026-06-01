package com.school.studentmanagement.student.service;

import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.global.enums.EmploymentStatus;
import com.school.studentmanagement.global.enums.Gender;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.enums.UserStatus;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.storage.FileStorageService;
import com.school.studentmanagement.global.util.AcademicCalendarUtil;
import com.school.studentmanagement.student.dto.StudentContactUpdateRequest;
import com.school.studentmanagement.student.dto.StudentProfileResponse;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.teacher.entity.Teacher;
import com.school.studentmanagement.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TeacherStudentProfileServiceTest {

    @InjectMocks private TeacherStudentProfileService service;
    @Mock private StudentAffiliationRepository studentAffiliationRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private AcademicCalendarUtil academicCalendarUtil;

    private static final Long HOMEROOM_TEACHER_ID = 100L;
    private static final Long OTHER_TEACHER_ID = 999L;
    private static final Long STUDENT_ID = 1L;
    private static final int YEAR = 2026;
    private static final int SEMESTER = 1;

    private Teacher homeroomTeacher;
    private Student student;
    private Classroom classroom;
    private StudentAffiliation affiliation;

    @BeforeEach
    void setUp() {
        lenient().when(academicCalendarUtil.getCurrentAcademicYear()).thenReturn(YEAR);
        lenient().when(academicCalendarUtil.getCurrentSemester()).thenReturn(SEMESTER);

        User teacherUser = User.builder().id(HOMEROOM_TEACHER_ID).name("최담임")
                .gender(Gender.MALE).role(UserRole.TEACHER).status(UserStatus.ACTIVE).build();
        homeroomTeacher = Teacher.builder().user(teacherUser).employeeNumber("EMP100")
                .officeLocation("본관").officePhoneNumber("02-000")
                .employmentStatus(EmploymentStatus.ACTIVE).build();
        ReflectionTestUtils.setField(homeroomTeacher, "id", HOMEROOM_TEACHER_ID);

        User studentUser = User.builder().id(STUDENT_ID).name("홍길동")
                .gender(Gender.MALE).role(UserRole.STUDENT).status(UserStatus.ACTIVE).build();
        student = Student.builder().id(STUDENT_ID).user(studentUser).enrollmentYear(2026)
                .address("서울시 강남구").phoneNumber("01011112222").build();

        classroom = Classroom.builder().academicYear(YEAR).semester(SEMESTER).grade(1).classNum(4)
                .homeroomTeacher(homeroomTeacher).build();
        ReflectionTestUtils.setField(classroom, "id", 200L);

        affiliation = StudentAffiliation.builder().student(student).classroom(classroom).studentNum(15).build();
    }

    @Test
    @DisplayName("성공: 담임 교사가 학생 프로필 조회 — 이름/학년/반/번호/주소/전화/담임 모두 포함")
    void getProfile_success() {
        given(studentAffiliationRepository.findWithAllDetails(STUDENT_ID, YEAR, SEMESTER))
                .willReturn(Optional.of(affiliation));
        given(fileStorageService.presignedGetUrl(null)).willReturn(null);

        StudentProfileResponse res = service.getProfile(STUDENT_ID, HOMEROOM_TEACHER_ID);

        assertThat(res.getName()).isEqualTo("홍길동");
        assertThat(res.getGrade()).isEqualTo(1);
        assertThat(res.getClassNum()).isEqualTo(4);
        assertThat(res.getStudentNum()).isEqualTo(15);
        assertThat(res.getAddress()).isEqualTo("서울시 강남구");
        assertThat(res.getPhoneNumber()).isEqualTo("01011112222");
        assertThat(res.getHomeroomTeacherName()).isEqualTo("최담임");
        assertThat(res.getEnrollmentYear()).isEqualTo(2026);
    }

    @Test
    @DisplayName("실패: 담임이 아닌 교사 → ACCESS_DENIED")
    void getProfile_fail_notHomeroom() {
        given(studentAffiliationRepository.findWithAllDetails(STUDENT_ID, YEAR, SEMESTER))
                .willReturn(Optional.of(affiliation));

        assertThatThrownBy(() -> service.getProfile(STUDENT_ID, OTHER_TEACHER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("담임 교사만 학생 프로필을 조회/수정할 수 있습니다");
    }

    @Test
    @DisplayName("실패: 현재 학기 소속 없음 → STUDENT_NOT_ASSIGNED")
    void getProfile_fail_noAffiliation() {
        given(studentAffiliationRepository.findWithAllDetails(STUDENT_ID, YEAR, SEMESTER))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfile(STUDENT_ID, HOMEROOM_TEACHER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("해당 학기에 배정된 학생이 아닙니다");
    }

    @Test
    @DisplayName("실패: 담임 미배정 학급(homeroomTeacher null) → ACCESS_DENIED")
    void getProfile_fail_homeroomNull() {
        Classroom noHomeroom = Classroom.builder().academicYear(YEAR).semester(SEMESTER).grade(1).classNum(3).build();
        StudentAffiliation aff = StudentAffiliation.builder().student(student).classroom(noHomeroom).studentNum(10).build();
        given(studentAffiliationRepository.findWithAllDetails(STUDENT_ID, YEAR, SEMESTER))
                .willReturn(Optional.of(aff));

        assertThatThrownBy(() -> service.getProfile(STUDENT_ID, HOMEROOM_TEACHER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("담임 교사만 학생 프로필을 조회/수정할 수 있습니다");
    }

    @Test
    @DisplayName("성공: 부분 갱신 — phoneNumber만 보내면 address는 그대로 유지")
    void updateContact_partialUpdate() {
        given(studentAffiliationRepository.findWithAllDetails(STUDENT_ID, YEAR, SEMESTER))
                .willReturn(Optional.of(affiliation));
        given(fileStorageService.presignedGetUrl(null)).willReturn(null);

        StudentContactUpdateRequest req = new StudentContactUpdateRequest();
        ReflectionTestUtils.setField(req, "phoneNumber", "01099998888");

        StudentProfileResponse res = service.updateContact(STUDENT_ID, HOMEROOM_TEACHER_ID, req);

        assertThat(res.getPhoneNumber()).isEqualTo("01099998888");
        assertThat(res.getAddress()).isEqualTo("서울시 강남구"); // 변경 없음
        assertThat(student.getPhoneNumber()).isEqualTo("01099998888");
    }

    @Test
    @DisplayName("실패: 담임이 아닌 교사의 수정 시도 → ACCESS_DENIED")
    void updateContact_fail_notHomeroom() {
        given(studentAffiliationRepository.findWithAllDetails(STUDENT_ID, YEAR, SEMESTER))
                .willReturn(Optional.of(affiliation));
        StudentContactUpdateRequest req = new StudentContactUpdateRequest();
        ReflectionTestUtils.setField(req, "address", "악의적 변경");

        assertThatThrownBy(() -> service.updateContact(STUDENT_ID, OTHER_TEACHER_ID, req))
                .isInstanceOf(BusinessException.class)
                .hasMessage("담임 교사만 학생 프로필을 조회/수정할 수 있습니다");
        assertThat(student.getAddress()).isEqualTo("서울시 강남구"); // 변경되지 않음
    }
}
