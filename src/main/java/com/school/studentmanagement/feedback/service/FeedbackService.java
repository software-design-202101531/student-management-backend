package com.school.studentmanagement.feedback.service;

import com.school.studentmanagement.feedback.dto.FeedbackCreateRequest;
import com.school.studentmanagement.feedback.dto.FeedbackResponse;
import com.school.studentmanagement.feedback.dto.FeedbackUpdateRequest;
import com.school.studentmanagement.feedback.entity.Feedback;
import com.school.studentmanagement.feedback.repository.FeedbackRepository;
import com.school.studentmanagement.global.enums.FeedbackStatus;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.parent.repository.ParentStudentMappingRepository;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.student.repository.StudentRepository;
import com.school.studentmanagement.teacher.entity.Teacher;
import com.school.studentmanagement.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ParentStudentMappingRepository parentStudentMappingRepository;

    // 피드백 생성 (교사 전용) — 최초 상태는 항상 DRAFT
    @Transactional
    public FeedbackResponse createFeedback(Long teacherId, FeedbackCreateRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_NOT_FOUND));

        boolean isPublic = Boolean.TRUE.equals(request.getIsPublic());

        Feedback feedback = Feedback.create(
                teacher, student, request.getCategory(), request.getContent(), isPublic);
        feedbackRepository.save(feedback);

        return FeedbackResponse.from(feedback);
    }

    // 피드백 목록 조회 — 요청자 권한(role)에 따라 노출 범위 분기
    public List<FeedbackResponse> getStudentFeedbacks(Long studentId, Long requesterId, UserRole role) {
        List<Feedback> feedbacks = switch (role) {
            // 교사: 임시저장/비공개 포함 전체 조회
            case TEACHER -> feedbackRepository.findAllByStudentId(studentId);
            // 학생: 본인 데이터만, 그중 발행 완료 + 공개 건만
            case STUDENT -> {
                if (!studentId.equals(requesterId)) {
                    throw new BusinessException(ErrorCode.ACCESS_DENIED, "본인의 피드백만 조회할 수 있습니다");
                }
                yield feedbackRepository.findVisibleByStudentId(studentId, FeedbackStatus.PUBLISHED);
            }
            // 학부모: 연결된 자녀만, 그중 발행 완료 + 공개 건만
            case PARENT -> {
                if (!parentStudentMappingRepository.existsByParentIdAndStudentId(requesterId, studentId)) {
                    throw new BusinessException(ErrorCode.ACCESS_DENIED, "연결된 자녀가 아닙니다");
                }
                yield feedbackRepository.findVisibleByStudentId(studentId, FeedbackStatus.PUBLISHED);
            }
            default -> throw new BusinessException(ErrorCode.ACCESS_DENIED);
        };

        return feedbacks.stream()
                .map(FeedbackResponse::from)
                .toList();
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
        // 비고: 향후 알림 시스템 도입 시 이 지점에서 비동기 알림 이벤트를 트리거할 수 있음
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
