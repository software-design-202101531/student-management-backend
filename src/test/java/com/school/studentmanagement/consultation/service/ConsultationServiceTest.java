package com.school.studentmanagement.consultation.service;

import com.school.studentmanagement.consultation.dto.ConsultationResponse;
import com.school.studentmanagement.consultation.dto.ConsultationUpdateRequest;
import com.school.studentmanagement.consultation.entity.Consultation;
import com.school.studentmanagement.consultation.repository.ConsultationRepository;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.enums.ConsultationVisibility;
import com.school.studentmanagement.global.enums.EmploymentStatus;
import com.school.studentmanagement.global.enums.Gender;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.enums.UserStatus;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.student.repository.StudentRepository;
import com.school.studentmanagement.teacher.entity.Teacher;
import com.school.studentmanagement.teacher.repository.TeacherRepository;
import com.school.studentmanagement.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ConsultationServiceTest {

    @InjectMocks private ConsultationService consultationService;
    @Mock private ConsultationRepository consultationRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private com.school.studentmanagement.global.validation.TeacherStudentRelationValidator teacherStudentRelationValidator;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private static final Long TEACHER_ID = 1L;
    private static final Long STUDENT_ID = 10L;
    private final Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "consultationDate"));

    @Nested
    @DisplayName("교사 간 상담 검색 (searchConsultations)")
    class SearchConsultationsTest {

        @Test
        @DisplayName("교사 요청: isAdmin=false, 필터·기간 그대로 전달")
        void teacher_filters_passedThrough() {
            Consultation c = consultation(100L, teacher(TEACHER_ID, "김교사"), student(STUDENT_ID));
            given(consultationRepository.searchConsultations(
                    eq(false), eq(TEACHER_ID),
                    eq(STUDENT_ID), eq(5L), eq(ConsultationVisibility.ALL_TEACHERS), eq("진로"),
                    any(LocalDateTime.class), any(LocalDateTime.class), eq(pageable)))
                    .willReturn(new PageImpl<>(List.of(c), pageable, 1));

            LocalDate from = LocalDate.of(2026, 3, 1);
            LocalDate to = LocalDate.of(2026, 5, 31);

            var page = consultationService.searchConsultations(
                    TEACHER_ID, UserRole.TEACHER,
                    STUDENT_ID, 5L, ConsultationVisibility.ALL_TEACHERS,
                    "진로", from, to, pageable);

            assertThat(page.getTotalElements()).isEqualTo(1);
            assertThat(page.getContent().get(0).getConsultationId()).isEqualTo(100L);

            // 기간 보정 검증: from -> 00:00:00, to -> 23:59:59.999999999
            ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> toCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(consultationRepository).searchConsultations(
                    eq(false), eq(TEACHER_ID),
                    eq(STUDENT_ID), eq(5L), eq(ConsultationVisibility.ALL_TEACHERS), eq("진로"),
                    fromCaptor.capture(), toCaptor.capture(), eq(pageable));
            assertThat(fromCaptor.getValue()).isEqualTo(from.atStartOfDay());
            assertThat(toCaptor.getValue()).isEqualTo(to.atTime(LocalTime.MAX));
        }

        @Test
        @DisplayName("관리자 요청: isAdmin=true 가 전달돼 권한 통과")
        void admin_isAdminTrue() {
            given(consultationRepository.searchConsultations(
                    eq(true), eq(99L), eq(null), eq(null), eq(null), eq(null),
                    eq(null), eq(null), eq(pageable)))
                    .willReturn(new PageImpl<>(List.of(), pageable, 0));

            var page = consultationService.searchConsultations(
                    99L, UserRole.ADMIN, null, null, null, null, null, null, pageable);

            assertThat(page.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("키워드 공백/빈 문자열은 null로 정규화 (LIKE '%%' 회피)")
        void keyword_blank_normalizedToNull() {
            given(consultationRepository.searchConsultations(
                    eq(false), eq(TEACHER_ID), eq(null), eq(null), eq(null),
                    eq(null), eq(null), eq(null), eq(pageable)))
                    .willReturn(new PageImpl<>(List.of(), pageable, 0));

            consultationService.searchConsultations(
                    TEACHER_ID, UserRole.TEACHER, null, null, null,
                    "   ", null, null, pageable);

            ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
            verify(consultationRepository).searchConsultations(
                    eq(false), eq(TEACHER_ID), eq(null), eq(null), eq(null),
                    keywordCaptor.capture(), eq(null), eq(null), eq(pageable));
            assertThat(keywordCaptor.getValue()).isNull();
        }

        @Test
        @DisplayName("키워드 양 끝 공백은 trim 되어 전달")
        void keyword_trimmed() {
            given(consultationRepository.searchConsultations(
                    eq(false), eq(TEACHER_ID), eq(null), eq(null), eq(null),
                    eq("진로"), eq(null), eq(null), eq(pageable)))
                    .willReturn(new PageImpl<>(List.of(), pageable, 0));

            consultationService.searchConsultations(
                    TEACHER_ID, UserRole.TEACHER, null, null, null,
                    "  진로  ", null, null, pageable);

            verify(consultationRepository).searchConsultations(
                    eq(false), eq(TEACHER_ID), eq(null), eq(null), eq(null),
                    eq("진로"), eq(null), eq(null), eq(pageable));
        }

        @Test
        @DisplayName("응답 매핑: Page<Consultation> -> Page<ConsultationResponse>")
        void responseMapping() {
            Consultation c = consultation(101L, teacher(TEACHER_ID, "박교사"), student(STUDENT_ID));
            given(consultationRepository.searchConsultations(
                    any(boolean.class), any(), any(), any(), any(),
                    any(), any(), any(), eq(pageable)))
                    .willReturn(new PageImpl<>(List.of(c), pageable, 1));

            var page = consultationService.searchConsultations(
                    TEACHER_ID, UserRole.TEACHER, null, null, null, null, null, null, pageable);

            ConsultationResponse res = page.getContent().get(0);
            assertThat(res.getConsultationId()).isEqualTo(101L);
            assertThat(res.getTeacherName()).isEqualTo("박교사");
            assertThat(res.getVisibility()).isEqualTo(ConsultationVisibility.RESTRICTED);
        }
    }

    @Nested
    @DisplayName("상담 내역 수정 (updateConsultation)")
    class UpdateConsultationTest {

        @Test
        @DisplayName("성공: 작성자 본인이 본문/일시/계획/공개범위 갱신")
        void update_author_success() {
            Consultation c = consultation(200L, teacher(TEACHER_ID, "김교사"), student(STUDENT_ID));
            given(consultationRepository.findById(200L)).willReturn(Optional.of(c));

            LocalDateTime newDate = LocalDateTime.of(2026, 5, 28, 14, 30);
            ConsultationUpdateRequest req = ConsultationUpdateRequest.builder()
                    .consultationDate(newDate).content("수정된 내용").nextPlan("수정된 계획")
                    .visibility(ConsultationVisibility.ALL_TEACHERS).build();

            ConsultationResponse res = consultationService.updateConsultation(200L, TEACHER_ID, req);

            assertThat(res.getConsultationDate()).isEqualTo(newDate);
            assertThat(res.getContent()).isEqualTo("수정된 내용");
            assertThat(res.getNextPlan()).isEqualTo("수정된 계획");
            assertThat(res.getVisibility()).isEqualTo(ConsultationVisibility.ALL_TEACHERS);
            verify(eventPublisher).publishEvent(any(
                    com.school.studentmanagement.consultation.event.ConsultationUpdatedEvent.class));
        }

        @Test
        @DisplayName("실패: 작성자가 아니면 ACCESS_DENIED")
        void update_notAuthor_denied() {
            Consultation c = consultation(200L, teacher(TEACHER_ID, "김교사"), student(STUDENT_ID));
            given(consultationRepository.findById(200L)).willReturn(Optional.of(c));

            ConsultationUpdateRequest req = ConsultationUpdateRequest.builder()
                    .consultationDate(LocalDateTime.of(2026, 5, 28, 14, 30))
                    .content("x").visibility(ConsultationVisibility.RESTRICTED).build();

            assertThatThrownBy(() -> consultationService.updateConsultation(200L, 99L, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("작성자 본인만 수정할 수 있습니다");
        }

        @Test
        @DisplayName("실패: 상담이 없으면 CONSULTATION_NOT_FOUND")
        void update_notFound() {
            given(consultationRepository.findById(404L)).willReturn(Optional.empty());

            ConsultationUpdateRequest req = ConsultationUpdateRequest.builder()
                    .consultationDate(LocalDateTime.now()).content("x")
                    .visibility(ConsultationVisibility.RESTRICTED).build();

            assertThatThrownBy(() -> consultationService.updateConsultation(404L, TEACHER_ID, req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("nextPlan 빈 문자열 입력 시 null 로 비움")
        void update_nextPlan_blank_clearsField() {
            Consultation c = consultation(200L, teacher(TEACHER_ID, "김교사"), student(STUDENT_ID));
            given(consultationRepository.findById(200L)).willReturn(Optional.of(c));

            ConsultationUpdateRequest req = ConsultationUpdateRequest.builder()
                    .consultationDate(LocalDateTime.of(2026, 5, 1, 10, 0))
                    .content("내용 유지").nextPlan("   ")
                    .visibility(ConsultationVisibility.RESTRICTED).build();

            ConsultationResponse res = consultationService.updateConsultation(200L, TEACHER_ID, req);
            assertThat(res.getNextPlan()).isNull();
        }
    }

    // ─── 헬퍼 ──────────────────────────────────────────────────────────────
    private Teacher teacher(Long id, String name) {
        User u = User.builder().id(id).name(name).gender(Gender.MALE)
                .role(UserRole.TEACHER).status(UserStatus.ACTIVE).build();
        Teacher t = Teacher.builder().user(u).employeeNumber("EMP" + id)
                .officeLocation("본관").officePhoneNumber("02-000")
                .employmentStatus(EmploymentStatus.ACTIVE).build();
        ReflectionTestUtils.setField(t, "id", id);
        return t;
    }

    private Student student(Long id) {
        User u = User.builder().id(id).name("학생" + id).gender(Gender.MALE)
                .role(UserRole.STUDENT).status(UserStatus.ACTIVE).build();
        return Student.builder().id(id).user(u).enrollmentYear(2026).build();
    }

    private Consultation consultation(Long id, Teacher t, Student s) {
        Consultation c = Consultation.create(t, s, LocalDateTime.of(2026, 5, 1, 10, 0),
                "상담 내용", "다음 계획", ConsultationVisibility.RESTRICTED);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }
}
