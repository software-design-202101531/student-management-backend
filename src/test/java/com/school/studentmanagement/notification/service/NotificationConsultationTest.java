package com.school.studentmanagement.notification.service;

import com.school.studentmanagement.consultation.entity.Consultation;
import com.school.studentmanagement.consultation.repository.ConsultationRepository;
import com.school.studentmanagement.global.enums.ConsultationVisibility;
import com.school.studentmanagement.global.enums.EmploymentStatus;
import com.school.studentmanagement.global.enums.Gender;
import com.school.studentmanagement.global.enums.NotificationType;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.enums.UserStatus;
import com.school.studentmanagement.notification.entity.Notification;
import com.school.studentmanagement.notification.repository.NotificationRepository;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.teacher.entity.Teacher;
import com.school.studentmanagement.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("상담 변경 → 담임 교사 알림 생성 (NotificationService)")
class NotificationConsultationTest {

    @InjectMocks private NotificationService notificationService;
    @Mock private NotificationRepository notificationRepository;
    @Mock private ConsultationRepository consultationRepository;
    @Mock private NotificationUnreadCountCache unreadCountCache;
    // 사용되지 않는 협력자들 (생성자 주입 충족용)
    @Mock private com.school.studentmanagement.grade.repository.StudentGradeRepository studentGradeRepository;
    @Mock private com.school.studentmanagement.parent.repository.ParentStudentMappingRepository parentStudentMappingRepository;
    @Mock private com.school.studentmanagement.grade.repository.ExamRepository examRepository;
    @Mock private com.school.studentmanagement.feedback.repository.FeedbackRepository feedbackRepository;
    @Mock private com.school.studentmanagement.assignment.repository.AssignmentRepository assignmentRepository;
    @Mock private com.school.studentmanagement.assignment.repository.SubmissionRepository submissionRepository;
    @Mock private com.school.studentmanagement.classroom.repository.StudentAffiliationRepository studentAffiliationRepository;

    private static final Long AUTHOR_ID = 1L;
    private static final Long HOMEROOM_ID = 2L;
    private static final Long STUDENT_ID = 10L;
    private static final Long CONSULTATION_ID = 100L;

    @Test
    @DisplayName("비담임이 작성하면 학생의 담임 교사 1명에게 알림 생성")
    void created_notifiesHomeroomTeacher() {
        Teacher author = teacher(AUTHOR_ID, "김교사");
        Teacher homeroom = teacher(HOMEROOM_ID, "이담임");
        Consultation c = consultation(author, student(STUDENT_ID, homeroom));
        given(consultationRepository.findByIdWithParticipants(CONSULTATION_ID)).willReturn(Optional.of(c));

        notificationService.createForConsultationCreated(CONSULTATION_ID);

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.captor();
        verify(notificationRepository).saveAll(captor.capture());
        List<Notification> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        Notification n = saved.get(0);
        assertThat(n.getRecipientUserId()).isEqualTo(HOMEROOM_ID);
        assertThat(n.getType()).isEqualTo(NotificationType.CONSULTATION_CREATED);
        assertThat(n.getReferenceId()).isEqualTo(CONSULTATION_ID);
        assertThat(n.getContent()).contains("김교사", "학생10", "등록");
        verify(unreadCountCache).evict(HOMEROOM_ID);
    }

    @Test
    @DisplayName("수정 알림은 CONSULTATION_UPDATED 타입으로 생성")
    void updated_usesUpdatedType() {
        Teacher author = teacher(AUTHOR_ID, "김교사");
        Teacher homeroom = teacher(HOMEROOM_ID, "이담임");
        Consultation c = consultation(author, student(STUDENT_ID, homeroom));
        given(consultationRepository.findByIdWithParticipants(CONSULTATION_ID)).willReturn(Optional.of(c));

        notificationService.createForConsultationUpdated(CONSULTATION_ID);

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.captor();
        verify(notificationRepository).saveAll(captor.capture());
        assertThat(captor.getValue().get(0).getType()).isEqualTo(NotificationType.CONSULTATION_UPDATED);
        assertThat(captor.getValue().get(0).getContent()).contains("수정");
    }

    @Test
    @DisplayName("작성자 본인이 담임이면 알림을 생성하지 않음")
    void authorIsHomeroom_noNotification() {
        Teacher author = teacher(AUTHOR_ID, "김담임");
        Consultation c = consultation(author, student(STUDENT_ID, author)); // 담임 == 작성자
        given(consultationRepository.findByIdWithParticipants(CONSULTATION_ID)).willReturn(Optional.of(c));

        notificationService.createForConsultationCreated(CONSULTATION_ID);

        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("담임 미배정이면 알림을 생성하지 않음")
    void noHomeroom_noNotification() {
        Teacher author = teacher(AUTHOR_ID, "김교사");
        Consultation c = consultation(author, student(STUDENT_ID, null));
        given(consultationRepository.findByIdWithParticipants(CONSULTATION_ID)).willReturn(Optional.of(c));

        notificationService.createForConsultationCreated(CONSULTATION_ID);

        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("상담이 없으면(경합 삭제) 조용히 종료")
    void notFound_noNotification() {
        given(consultationRepository.findByIdWithParticipants(CONSULTATION_ID)).willReturn(Optional.empty());

        notificationService.createForConsultationCreated(CONSULTATION_ID);

        verify(notificationRepository, never()).saveAll(any());
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

    private Student student(Long id, Teacher homeroom) {
        User u = User.builder().id(id).name("학생" + id).gender(Gender.MALE)
                .role(UserRole.STUDENT).status(UserStatus.ACTIVE).build();
        return Student.builder().id(id).user(u).homeroomTeacher(homeroom).enrollmentYear(2026).build();
    }

    private Consultation consultation(Teacher author, Student s) {
        Consultation c = Consultation.create(author, s, LocalDateTime.of(2026, 5, 1, 10, 0),
                "상담 내용", "다음 계획", ConsultationVisibility.RESTRICTED);
        ReflectionTestUtils.setField(c, "id", CONSULTATION_ID);
        return c;
    }
}
