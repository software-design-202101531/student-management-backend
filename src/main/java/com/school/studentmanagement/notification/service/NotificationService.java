package com.school.studentmanagement.notification.service;

import com.school.studentmanagement.assignment.entity.Assignment;
import com.school.studentmanagement.assignment.entity.Submission;
import com.school.studentmanagement.assignment.repository.AssignmentRepository;
import com.school.studentmanagement.assignment.repository.SubmissionRepository;
import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.consultation.entity.Consultation;
import com.school.studentmanagement.consultation.repository.ConsultationRepository;
import com.school.studentmanagement.feedback.entity.Feedback;
import com.school.studentmanagement.feedback.repository.FeedbackRepository;
import com.school.studentmanagement.global.enums.NotificationStatus;
import com.school.studentmanagement.global.enums.NotificationType;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.grade.entity.Exam;
import com.school.studentmanagement.grade.repository.ExamRepository;
import com.school.studentmanagement.grade.repository.StudentGradeRepository;
import com.school.studentmanagement.notification.dto.NotificationResponse;
import com.school.studentmanagement.notification.entity.Notification;
import com.school.studentmanagement.notification.repository.NotificationRepository;
import com.school.studentmanagement.parent.repository.ParentStudentMappingRepository;
import com.school.studentmanagement.teacher.entity.Teacher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final StudentGradeRepository studentGradeRepository;
    private final ParentStudentMappingRepository parentStudentMappingRepository;
    private final ExamRepository examRepository;
    private final FeedbackRepository feedbackRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final StudentAffiliationRepository studentAffiliationRepository;
    private final ConsultationRepository consultationRepository;
    private final NotificationUnreadCountCache unreadCountCache;

    // ===== 조회 (폴링) =====

    public Page<NotificationResponse> getMyNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::from);
    }

    // 미확인 개수 — Redis 캐시 우선, miss 시 DB 집계
    public long getUnreadCount(Long userId) {
        return unreadCountCache.getOrLoad(userId,
                () -> notificationRepository.countByRecipientUserIdAndStatus(userId, NotificationStatus.UNREAD));
    }

    // ===== 확인 처리 =====

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!notification.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "본인의 알림만 변경할 수 있습니다");
        }
        notification.markAsRead();
        unreadCountCache.evict(userId);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllReadByRecipient(userId, LocalDateTime.now());
        unreadCountCache.evict(userId);
    }

    // ===== 생성 (이벤트 리스너에서 커밋 이후 비동기 호출) =====

    // 시험 발행 → 성적이 있는 학생당 알림 1건(학부모 포함). 시험 단위 안내(과목·교사 미표기).
    @Transactional
    public void createForExamPublished(Long examId) {
        Exam exam = examRepository.findById(examId).orElse(null);
        if (exam == null) {
            return; // 발행 직후 삭제 등 경합 상황 방어
        }
        List<Long> studentIds = studentGradeRepository.findDistinctStudentIdsByExamId(examId);
        if (studentIds.isEmpty()) {
            return;
        }

        String title = "성적이 발행되었어요";
        String content = String.format("%d학년도 %d학기 %s 성적이 발행되었습니다.",
                exam.getAcademicYear(), exam.getSemester(), exam.getName());

        for (Long studentId : studentIds) {
            // linkUrl은 수신자(학생/학부모)가 해당 학생 성적으로 진입하도록 학생별로 구성
            String linkUrl = "/students/" + studentId + "/grades?examId=" + examId;
            saveAll(resolveRecipients(List.of(studentId)),
                    NotificationType.GRADE_PUBLISHED, title, content, linkUrl, examId);
        }
    }

    // 공개 피드백 발행 → 대상 학생 + 그 학부모에게 알림 (작성 교사명 표기)
    @Transactional
    public void createForFeedbackPublished(Long feedbackId) {
        Feedback feedback = feedbackRepository.findById(feedbackId).orElse(null);
        // 발행 + 공개 상태가 아니면 학생/학부모가 볼 수 없으므로 알림 생략
        if (feedback == null || !feedback.isPublished() || !feedback.isPublic()) {
            return;
        }

        Long studentId = feedback.getStudent().getId();
        String teacherName = feedback.getTeacher().getUser().getName();
        String title = "새 피드백이 등록되었어요";
        String content = String.format("%s 선생님이 새로운 피드백을 남겼습니다.", teacherName);
        String linkUrl = "/students/" + studentId + "/feedbacks";

        saveAll(resolveRecipients(List.of(studentId)),
                NotificationType.FEEDBACK_PUBLISHED, title, content, linkUrl, feedbackId);
    }

    // 과제 부여 → 해당 학급의 모든 학생(+학부모)에게 알림. 과목명·과제명 표기.
    @Transactional
    public void createForAssignmentCreated(Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId).orElse(null);
        if (assignment == null) {
            return; // 부여 직후 삭제 등 경합 방어
        }
        List<Long> studentIds = studentAffiliationRepository
                .findAllByClassroomId(assignment.getClassroom().getId()).stream()
                .map(a -> a.getStudent().getId())
                .toList();
        if (studentIds.isEmpty()) {
            return;
        }

        String title = "새 과제가 등록되었어요";
        String content = String.format("%s 과목 과제 '%s'가 등록되었습니다.",
                assignment.getSubject().getName(), assignment.getTitle());
        String linkUrl = "/student/me/assignments";

        saveAll(resolveRecipients(studentIds),
                NotificationType.ASSIGNMENT_CREATED, title, content, linkUrl, assignmentId);
    }

    // 과제 채점 → 해당 학생(+학부모)에게 알림. (점수는 알림에 노출하지 않고 진입 후 확인)
    @Transactional
    public void createForAssignmentGraded(Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission == null || !submission.isGraded()) {
            return;
        }
        Long studentId = submission.getStudent().getId();
        String title = "과제가 채점되었어요";
        String content = String.format("과제 '%s'가 채점되었습니다.", submission.getAssignment().getTitle());
        String linkUrl = "/student/me/assignments";

        saveAll(resolveRecipients(List.of(studentId)),
                NotificationType.ASSIGNMENT_GRADED, title, content, linkUrl, submission.getAssignment().getId());
    }

    // 상담 등록 → 대상 학생의 담임 교사에게 알림
    @Transactional
    public void createForConsultationCreated(Long consultationId) {
        notifyHomeroomTeacher(consultationId, NotificationType.CONSULTATION_CREATED,
                "새 상담 내역이 등록되었어요", "등록");
    }

    // 상담 수정 → 대상 학생의 담임 교사에게 알림
    @Transactional
    public void createForConsultationUpdated(Long consultationId) {
        notifyHomeroomTeacher(consultationId, NotificationType.CONSULTATION_UPDATED,
                "상담 내역이 수정되었어요", "수정");
    }

    // 상담 대상 학생의 담임 교사에게만 알림. 담임 미배정이거나 작성자 본인이 담임이면 생략.
    // 담임은 공개 범위(visibility)와 무관하게 해당 상담을 열람할 수 있어 알림-권한이 정합한다.
    private void notifyHomeroomTeacher(Long consultationId, NotificationType type,
                                       String title, String actionVerb) {
        Consultation consultation = consultationRepository.findByIdWithParticipants(consultationId).orElse(null);
        if (consultation == null) {
            return; // 변경 직후 삭제 등 경합 방어
        }
        Teacher homeroom = consultation.getStudent().getHomeroomTeacher();
        if (homeroom == null) {
            return; // 담임 미배정 → 수신자 없음
        }
        Long homeroomUserId = homeroom.getId(); // @MapsId: teacher.id == userId
        if (homeroomUserId.equals(consultation.getTeacher().getId())) {
            return; // 작성자 본인이 담임이면 알림 불필요
        }

        String teacherName = consultation.getTeacher().getUser().getName();
        String studentName = consultation.getStudent().getUser().getName();
        String content = String.format("%s 선생님이 %s 학생의 상담을 %s했습니다.", teacherName, studentName, actionVerb);
        String linkUrl = "/consultations/search?studentId=" + consultation.getStudent().getId();

        saveAll(Set.of(homeroomUserId), type, title, content, linkUrl, consultationId);
    }

    // 학생 본인(userId == studentId) + 연결된 학부모(userId == parentId)의 합집합
    private Set<Long> resolveRecipients(List<Long> studentIds) {
        Set<Long> recipients = new HashSet<>(studentIds);
        for (Long studentId : studentIds) {
            recipients.addAll(parentStudentMappingRepository.findParentIdsByStudentId(studentId));
        }
        return recipients;
    }

    private void saveAll(Set<Long> recipientUserIds, NotificationType type,
                         String title, String content, String linkUrl, Long referenceId) {
        if (recipientUserIds.isEmpty()) {
            return;
        }
        List<Notification> notifications = recipientUserIds.stream()
                .map(uid -> Notification.builder()
                        .recipientUserId(uid)
                        .type(type)
                        .title(title)
                        .content(content)
                        .linkUrl(linkUrl)
                        .referenceId(referenceId)
                        .build())
                .toList();
        notificationRepository.saveAll(notifications);
        recipientUserIds.forEach(unreadCountCache::evict);
    }
}
