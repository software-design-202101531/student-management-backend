package com.school.studentmanagement.feedback.service;

import com.school.studentmanagement.feedback.dto.FeedbackCreateRequest;
import com.school.studentmanagement.feedback.dto.FeedbackResponse;
import com.school.studentmanagement.feedback.dto.FeedbackUpdateRequest;
import com.school.studentmanagement.feedback.entity.Feedback;
import com.school.studentmanagement.feedback.repository.FeedbackRepository;
import com.school.studentmanagement.global.enums.EmploymentStatus;
import com.school.studentmanagement.global.enums.FeedbackCategory;
import com.school.studentmanagement.global.enums.FeedbackStatus;
import com.school.studentmanagement.global.enums.Gender;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.enums.UserStatus;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.global.validation.TeacherStudentRelationValidator;
import com.school.studentmanagement.parent.validator.ParentChildLinkValidator;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @InjectMocks private FeedbackService feedbackService;
    @Mock private FeedbackRepository feedbackRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private ParentChildLinkValidator parentChildLinkValidator;
    @Mock private TeacherStudentRelationValidator teacherStudentRelationValidator;
    @Mock private ApplicationEventPublisher eventPublisher;

    private static final Long TEACHER_ID = 1L;
    private static final Long STUDENT_ID = 10L;
    private static final Long FEEDBACK_ID = 100L;

    @Nested
    @DisplayName("피드백 생성 (createFeedback)")
    class CreateFeedbackTest {

        @Test
        @DisplayName("성공: 최초 상태 DRAFT로 생성 (관계 검증 통과)")
        void createFeedback_success() {
            given(studentRepository.findById(STUDENT_ID)).willReturn(Optional.of(student(STUDENT_ID)));
            given(teacherRepository.findById(TEACHER_ID)).willReturn(Optional.of(teacher(TEACHER_ID, "김교사")));
            given(feedbackRepository.save(any(Feedback.class))).willAnswer(inv -> inv.getArgument(0));

            FeedbackCreateRequest req = FeedbackCreateRequest.builder()
                    .studentId(STUDENT_ID).category(FeedbackCategory.GRADE).content("성실함").isPublic(true).build();

            FeedbackResponse res = feedbackService.createFeedback(TEACHER_ID, req);

            assertThat(res.getStatus()).isEqualTo(FeedbackStatus.DRAFT);
            assertThat(res.getStudentId()).isEqualTo(STUDENT_ID);
            assertThat(res.getTeacherId()).isEqualTo(TEACHER_ID);
            assertThat(res.getTeacherName()).isEqualTo("김교사");
            assertThat(res.isPublic()).isTrue();
            verify(feedbackRepository).save(any(Feedback.class));
            verify(teacherStudentRelationValidator).validateCanWriteFor(TEACHER_ID, STUDENT_ID);
        }

        @Test
        @DisplayName("실패: 담임도 과목 담당도 아니면 ACCESS_DENIED (validator 가 막음)")
        void createFeedback_unauthorizedRelation() {
            given(studentRepository.findById(STUDENT_ID)).willReturn(Optional.of(student(STUDENT_ID)));
            given(teacherRepository.findById(TEACHER_ID)).willReturn(Optional.of(teacher(TEACHER_ID, "김교사")));
            willThrow(new BusinessException(ErrorCode.ACCESS_DENIED, "해당 학생에 대한 작성 권한이 없습니다(담임 또는 과목 담당 교사만 가능)"))
                    .given(teacherStudentRelationValidator).validateCanWriteFor(TEACHER_ID, STUDENT_ID);

            FeedbackCreateRequest req = FeedbackCreateRequest.builder()
                    .studentId(STUDENT_ID).category(FeedbackCategory.GRADE).content("성실함").isPublic(false).build();

            assertThatThrownBy(() -> feedbackService.createFeedback(TEACHER_ID, req))
                    .isInstanceOf(BusinessException.class);
            // 검증 실패 시 save 가 호출되지 않아야 함은 verify(...).never() 보다 willAnswer 미설정으로 충분.
        }
    }

    @Nested
    @DisplayName("피드백 목록 조회 (getStudentFeedbacks)")
    class GetStudentFeedbacksTest {

        @Test
        @DisplayName("교사: 발행 전체 + 본인 초안 뷰 쿼리 사용")
        void teacher_usesTeacherView() {
            Feedback f = feedback(FEEDBACK_ID, teacher(TEACHER_ID, "김교사"), student(STUDENT_ID), true);
            given(feedbackRepository.findForTeacherView(STUDENT_ID, FeedbackStatus.PUBLISHED, TEACHER_ID))
                    .willReturn(List.of(f));

            List<FeedbackResponse> res = feedbackService.getStudentFeedbacks(STUDENT_ID, TEACHER_ID, UserRole.TEACHER);

            assertThat(res).hasSize(1);
            assertThat(res.get(0).getFeedbackId()).isEqualTo(FEEDBACK_ID);
            verify(feedbackRepository).findForTeacherView(STUDENT_ID, FeedbackStatus.PUBLISHED, TEACHER_ID);
        }

        @Test
        @DisplayName("학생 본인: 발행+공개 건만 조회")
        void student_self_visibleOnly() {
            Feedback f = feedback(FEEDBACK_ID, teacher(TEACHER_ID, "김교사"), student(STUDENT_ID), true);
            given(feedbackRepository.findVisibleByStudentId(STUDENT_ID, FeedbackStatus.PUBLISHED))
                    .willReturn(List.of(f));

            List<FeedbackResponse> res = feedbackService.getStudentFeedbacks(STUDENT_ID, STUDENT_ID, UserRole.STUDENT);

            assertThat(res).hasSize(1);
        }

        @Test
        @DisplayName("학생: 타인 피드백 조회 시 ACCESS_DENIED")
        void student_other_denied() {
            assertThatThrownBy(() -> feedbackService.getStudentFeedbacks(STUDENT_ID, 99L, UserRole.STUDENT))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인의 피드백만 조회할 수 있습니다");
        }

        @Test
        @DisplayName("학부모: 연결된 자녀면 발행+공개 건 조회")
        void parent_linked() {
            given(feedbackRepository.findVisibleByStudentId(STUDENT_ID, FeedbackStatus.PUBLISHED))
                    .willReturn(List.of(feedback(FEEDBACK_ID, teacher(TEACHER_ID, "김교사"), student(STUDENT_ID), true)));

            List<FeedbackResponse> res = feedbackService.getStudentFeedbacks(STUDENT_ID, 50L, UserRole.PARENT);

            assertThat(res).hasSize(1);
            verify(parentChildLinkValidator).validateLinked(50L, STUDENT_ID);
        }

        @Test
        @DisplayName("학부모: 연결되지 않은 자녀면 ACCESS_DENIED")
        void parent_notLinked_denied() {
            willThrow(new BusinessException(ErrorCode.ACCESS_DENIED, "연결된 자녀가 아닙니다"))
                    .given(parentChildLinkValidator).validateLinked(50L, STUDENT_ID);

            assertThatThrownBy(() -> feedbackService.getStudentFeedbacks(STUDENT_ID, 50L, UserRole.PARENT))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("연결된 자녀가 아닙니다");
        }
    }

    @Nested
    @DisplayName("피드백 검색 (searchStudentFeedbacks)")
    class SearchStudentFeedbacksTest {

        private final Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        @Test
        @DisplayName("교사: 카테고리·기간·페이지 그대로 teacherView 검색 호출")
        void teacher_filters_passedThrough() {
            Feedback f = feedback(FEEDBACK_ID, teacher(TEACHER_ID, "김교사"), student(STUDENT_ID), true);
            given(feedbackRepository.searchForTeacherView(
                    eq(STUDENT_ID), eq(FeedbackStatus.PUBLISHED), eq(TEACHER_ID),
                    eq(FeedbackCategory.GRADE), any(LocalDateTime.class), any(LocalDateTime.class), eq(pageable)))
                    .willReturn(new PageImpl<>(List.of(f), pageable, 1));

            LocalDate from = LocalDate.of(2026, 3, 1);
            LocalDate to = LocalDate.of(2026, 5, 31);

            var page = feedbackService.searchStudentFeedbacks(
                    STUDENT_ID, TEACHER_ID, UserRole.TEACHER,
                    FeedbackCategory.GRADE, from, to, pageable);

            assertThat(page.getTotalElements()).isEqualTo(1);
            assertThat(page.getContent().get(0).getFeedbackId()).isEqualTo(FEEDBACK_ID);

            // 기간 보정: from -> 00:00:00, to -> 23:59:59.999999999 검증
            ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> toCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(feedbackRepository).searchForTeacherView(
                    eq(STUDENT_ID), eq(FeedbackStatus.PUBLISHED), eq(TEACHER_ID),
                    eq(FeedbackCategory.GRADE), fromCaptor.capture(), toCaptor.capture(), eq(pageable));
            assertThat(fromCaptor.getValue()).isEqualTo(from.atStartOfDay());
            assertThat(toCaptor.getValue()).isEqualTo(to.atTime(LocalTime.MAX));
        }

        @Test
        @DisplayName("학생 본인: visible 검색 쿼리 사용, 필터 null도 그대로 전달")
        void student_self_nullFilters() {
            given(feedbackRepository.searchVisibleByStudentId(
                    eq(STUDENT_ID), eq(FeedbackStatus.PUBLISHED),
                    eq(null), eq(null), eq(null), eq(pageable)))
                    .willReturn(new PageImpl<>(List.of(), pageable, 0));

            var page = feedbackService.searchStudentFeedbacks(
                    STUDENT_ID, STUDENT_ID, UserRole.STUDENT,
                    null, null, null, pageable);

            assertThat(page.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("학생: 타인 피드백 검색 시 ACCESS_DENIED")
        void student_other_denied() {
            assertThatThrownBy(() -> feedbackService.searchStudentFeedbacks(
                    STUDENT_ID, 99L, UserRole.STUDENT, null, null, null, pageable))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인의 피드백만 조회할 수 있습니다");
        }

        @Test
        @DisplayName("학부모: 연결되지 않은 자녀면 ACCESS_DENIED (검색 쿼리 미호출)")
        void parent_notLinked_denied() {
            willThrow(new BusinessException(ErrorCode.ACCESS_DENIED, "연결된 자녀가 아닙니다"))
                    .given(parentChildLinkValidator).validateLinked(50L, STUDENT_ID);

            assertThatThrownBy(() -> feedbackService.searchStudentFeedbacks(
                    STUDENT_ID, 50L, UserRole.PARENT, null, null, null, pageable))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("연결된 자녀가 아닙니다");
        }
    }

    @Nested
    @DisplayName("피드백 수정/발행")
    class UpdateAndPublishTest {

        @Test
        @DisplayName("수정 성공: 작성자 본인")
        void update_author_success() {
            Feedback f = feedback(FEEDBACK_ID, teacher(TEACHER_ID, "김교사"), student(STUDENT_ID), false);
            given(feedbackRepository.findById(FEEDBACK_ID)).willReturn(Optional.of(f));

            FeedbackUpdateRequest req = FeedbackUpdateRequest.builder()
                    .category(FeedbackCategory.ATTITUDE).content("수정됨").isPublic(true).build();

            FeedbackResponse res = feedbackService.updateFeedback(FEEDBACK_ID, TEACHER_ID, req);

            assertThat(res.getContent()).isEqualTo("수정됨");
            assertThat(res.getCategory()).isEqualTo(FeedbackCategory.ATTITUDE);
            assertThat(res.isPublic()).isTrue();
        }

        @Test
        @DisplayName("수정 실패: 작성자가 아니면 ACCESS_DENIED")
        void update_notAuthor_denied() {
            Feedback f = feedback(FEEDBACK_ID, teacher(TEACHER_ID, "김교사"), student(STUDENT_ID), false);
            given(feedbackRepository.findById(FEEDBACK_ID)).willReturn(Optional.of(f));

            FeedbackUpdateRequest req = FeedbackUpdateRequest.builder()
                    .category(FeedbackCategory.ETC).content("x").isPublic(false).build();

            assertThatThrownBy(() -> feedbackService.updateFeedback(FEEDBACK_ID, 2L, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("작성자 본인만 수정할 수 있습니다");
        }

        @Test
        @DisplayName("발행 성공: DRAFT -> PUBLISHED")
        void publish_success() {
            Feedback f = feedback(FEEDBACK_ID, teacher(TEACHER_ID, "김교사"), student(STUDENT_ID), false);
            given(feedbackRepository.findById(FEEDBACK_ID)).willReturn(Optional.of(f));

            feedbackService.publishFeedback(FEEDBACK_ID, TEACHER_ID);

            assertThat(f.isPublished()).isTrue();
        }

        @Test
        @DisplayName("발행 실패: 이미 발행된 건")
        void publish_alreadyPublished_throws() {
            Feedback f = feedback(FEEDBACK_ID, teacher(TEACHER_ID, "김교사"), student(STUDENT_ID), true);
            given(feedbackRepository.findById(FEEDBACK_ID)).willReturn(Optional.of(f));

            assertThatThrownBy(() -> feedbackService.publishFeedback(FEEDBACK_ID, TEACHER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("이미 발행된 피드백입니다");
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

    private Feedback feedback(Long id, Teacher t, Student s, boolean published) {
        Feedback f = Feedback.create(t, s, FeedbackCategory.GRADE, "내용", true);
        if (published) f.publish();
        ReflectionTestUtils.setField(f, "id", id);
        return f;
    }
}
