package com.school.studentmanagement.grade.service;

import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.grade.dto.GradeHistoryResponse;
import com.school.studentmanagement.grade.entity.GradeHistory;
import com.school.studentmanagement.grade.entity.StudentGrade;
import com.school.studentmanagement.grade.repository.GradeHistoryRepository;
import com.school.studentmanagement.grade.repository.StudentGradeRepository;
import com.school.studentmanagement.subject.repository.SubjectAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradeHistoryService {

    private final GradeHistoryRepository gradeHistoryRepository;
    private final StudentGradeRepository studentGradeRepository;
    private final StudentAffiliationRepository studentAffiliationRepository;
    private final SubjectAssignmentRepository subjectAssignmentRepository;

    public GradeHistoryResponse getHistory(Long gradeId, Long teacherId) {
        StudentGrade grade = studentGradeRepository.findById(gradeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GRADE_NOT_FOUND));

        validateTeacherCanViewHistory(teacherId, grade);

        List<GradeHistory> histories = gradeHistoryRepository
                .findByStudentGradeIdOrderByChangedAtDesc(gradeId);

        List<GradeHistoryResponse.HistoryEntry> entries = histories.stream()
                .map(h -> GradeHistoryResponse.HistoryEntry.builder()
                        .historyId(h.getId())
                        .beforeScore(h.getBeforeScore())
                        .afterScore(h.getAfterScore())
                        .changedByUserId(h.getChangedByUserId())
                        .changedByName(h.getChangedByName())
                        .reason(h.getReason())
                        .changedAt(h.getChangedAt())
                        .build())
                .toList();

        return GradeHistoryResponse.builder()
                .gradeId(grade.getId())
                .studentId(grade.getStudent().getId())
                .studentName(grade.getStudent().getUser().getName())
                .subjectName(grade.getSubject().getName())
                .examName(grade.getExam().getName())
                .histories(entries)
                .build();
    }

    private void validateTeacherCanViewHistory(Long teacherId, StudentGrade grade) {
        Long studentId = grade.getStudent().getId();
        Integer year = grade.getExam().getAcademicYear();
        Integer semester = grade.getExam().getSemester();

        StudentAffiliation aff = studentAffiliationRepository.findWithAllDetails(studentId, year, semester)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_ASSIGNED));

        boolean isHomeroom = aff.getClassroom().getHomeroomTeacher() != null
                && aff.getClassroom().getHomeroomTeacher().getId().equals(teacherId);
        if (isHomeroom) return;

        boolean hasSubjectAssignment = subjectAssignmentRepository
                .findValidAssignment(teacherId, aff.getClassroom().getId(),
                        grade.getSubject().getId(), year, semester)
                .isPresent();
        if (!hasSubjectAssignment) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "해당 성적의 변경 이력 조회 권한이 없습니다");
        }
    }
}
