package com.school.studentmanagement.feedback.service;

import com.school.studentmanagement.feedback.dto.FeedbackCreateRequest;
import com.school.studentmanagement.feedback.dto.FeedbackResponse;
import com.school.studentmanagement.feedback.dto.FeedbackUpdateRequest;
import com.school.studentmanagement.feedback.entity.Feedback;
import com.school.studentmanagement.feedback.event.FeedbackPublishedEvent;
import com.school.studentmanagement.feedback.repository.FeedbackRepository;
import com.school.studentmanagement.global.enums.FeedbackCategory;
import com.school.studentmanagement.global.enums.FeedbackStatus;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.global.validation.TeacherStudentRelationValidator;
import com.school.studentmanagement.parent.validator.ParentChildLinkValidator;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.student.repository.StudentRepository;
import com.school.studentmanagement.teacher.entity.Teacher;
import com.school.studentmanagement.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ParentChildLinkValidator parentChildLinkValidator;
    private final TeacherStudentRelationValidator teacherStudentRelationValidator;
    private final ApplicationEventPublisher eventPublisher;

    // 피드백 생성 (교사 전용) — 최초 상태는 항상 DRAFT.
    // 작성 권한은 담임 또는 과목 담당 교사로 제한(F3 정책, 2026-05-28 변경).
    @Transactional
    public FeedbackResponse createFeedback(Long teacherId, FeedbackCreateRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_NOT_FOUND));

        teacherStudentRelationValidator.validateCanWriteFor(teacherId, student.getId());

        boolean isPublic = Boolean.TRUE.equals(request.getIsPublic());

        Feedback feedback = Feedback.create(
                teacher, student, request.getCategory(), request.getContent(), isPublic);
        feedbackRepository.save(feedback);

        return FeedbackResponse.from(feedback);
    }

    // 피드백 목록 조회 — 요청자 권한(role)에 따라 노출 범위 분기
    public List<FeedbackResponse> getStudentFeedbacks(Long studentId, Long requesterId, UserRole role) {
        List<Feedback> feedbacks = switch (role) {
            // 교사: 발행 완료 건 전체 + 본인이 작성한 건(초안 포함). 타 교사 초안은 제외.
            case TEACHER -> feedbackRepository.findForTeacherView(studentId, FeedbackStatus.PUBLISHED, requesterId);
            // 학생: 본인 데이터만, 그중 발행 완료 + 공개 건만
            case STUDENT -> {
                if (!studentId.equals(requesterId)) {
                    throw new BusinessException(ErrorCode.ACCESS_DENIED, "본인의 피드백만 조회할 수 있습니다");
                }
                yield feedbackRepository.findVisibleByStudentId(studentId, FeedbackStatus.PUBLISHED);
            }
            // 학부모: 연결된 자녀만, 그중 발행 완료 + 공개 건만
            case PARENT -> {
                parentChildLinkValidator.validateLinked(requesterId, studentId);
                yield feedbackRepository.findVisibleByStudentId(studentId, FeedbackStatus.PUBLISHED);
            }
            default -> throw new BusinessException(ErrorCode.ACCESS_DENIED);
        };

        return feedbacks.stream()
                .map(FeedbackResponse::from)
                .toList();
    }

    // 피드백 검색 — 카테고리/기간 필터 + 페이지네이션. 권한별 노출 범위는 기존 분기와 동일.
    public Page<FeedbackResponse> searchStudentFeedbacks(Long studentId,
                                                         Long requesterId,
                                                         UserRole role,
                                                         FeedbackCategory category,
                                                         LocalDate from,
                                                         LocalDate to,
                                                         Pageable pageable) {
        // from/to는 해당 일자 전체를 포함하도록 시각 보정 (from 00:00:00, to 23:59:59.999999999)
        LocalDateTime fromDateTime = (from != null) ? from.atStartOfDay() : null;
        LocalDateTime toDateTime = (to != null) ? to.atTime(LocalTime.MAX) : null;

        Page<Feedback> page = switch (role) {
            case TEACHER -> feedbackRepository.searchForTeacherView(
                    studentId, FeedbackStatus.PUBLISHED, requesterId, category, fromDateTime, toDateTime, pageable);
            case STUDENT -> {
                if (!studentId.equals(requesterId)) {
                    throw new BusinessException(ErrorCode.ACCESS_DENIED, "본인의 피드백만 조회할 수 있습니다");
                }
                yield feedbackRepository.searchVisibleByStudentId(
                        studentId, FeedbackStatus.PUBLISHED, category, fromDateTime, toDateTime, pageable);
            }
            case PARENT -> {
                parentChildLinkValidator.validateLinked(requesterId, studentId);
                yield feedbackRepository.searchVisibleByStudentId(
                        studentId, FeedbackStatus.PUBLISHED, category, fromDateTime, toDateTime, pageable);
            }
            default -> throw new BusinessException(ErrorCode.ACCESS_DENIED);
        };

        return page.map(FeedbackResponse::from);
    }

    // 피드백 수정 (작성자 본인만) — 본문/분류/공개옵션 변경
    @Transactional
    public FeedbackResponse updateFeedback(Long feedbackId, Long teacherId, FeedbackUpdateRequest request) {
        Feedback feedback = findAuthoredFeedback(feedbackId, teacherId);

        feedback.update(request.getCategory(), request.getContent(), Boolean.TRUE.equals(request.getIsPublic()));

        return FeedbackResponse.from(feedback);
    }

    // 피드백 최종 발행 (작성자 본인만) — DRAFT -> PUBLISHED
    @Transactional
    public void publishFeedback(Long feedbackId, Long teacherId) {
        Feedback feedback = findAuthoredFeedback(feedbackId, teacherId);

        if (feedback.isPublished()) {
            throw new BusinessException(ErrorCode.FEEDBACK_ALREADY_PUBLISHED);
        }

        feedback.publish();
        // 공개(isPublic) 발행만 학생/학부모가 볼 수 있으므로 그 경우에만 알림 이벤트를 발행한다.
        // 실제 알림 생성은 커밋 이후(AFTER_COMMIT) 비동기로 처리된다.
        if (feedback.isPublic()) {
            eventPublisher.publishEvent(new FeedbackPublishedEvent(feedback.getId()));
        }

        // 분석 증분 적재 트리거 — 발행(공개 여부 무관, 집계는 PUBLISHED 기준) 후 커밋 시 RabbitMQ로 중계됨
        eventPublisher.publishEvent(new com.school.studentmanagement.analytics.event.AnalyticsSourceEvent(
                com.school.studentmanagement.analytics.event.AnalyticsRabbitConfig.RK_FEEDBACK_PUBLISHED,
                new com.school.studentmanagement.analytics.event.AnalyticsEventMessage(
                        feedback.getStudent().getId(), null)));
    }

    // 피드백 존재 + 작성자 본인 여부 검증
    private Feedback findAuthoredFeedback(Long feedbackId, Long teacherId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEEDBACK_NOT_FOUND));

        if (!feedback.isAuthor(teacherId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "작성자 본인만 수정할 수 있습니다");
        }

        return feedback;
    }
}
