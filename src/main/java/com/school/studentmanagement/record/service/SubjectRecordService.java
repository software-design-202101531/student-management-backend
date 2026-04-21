package com.school.studentmanagement.record.service;

import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.global.enums.RecordCategory;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.global.util.AcademicCalendarUtil;
import com.school.studentmanagement.record.entity.StudentRecord;
import com.school.studentmanagement.record.repository.StudentRecordRepository;
import com.school.studentmanagement.record.dto.SubjectRecordRequest;
import com.school.studentmanagement.record.dto.SubjectRecordResponse;
import com.school.studentmanagement.subject.entity.SubjectAssignment;
import com.school.studentmanagement.subject.repository.SubjectAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubjectRecordService {

    private final StudentRecordRepository studentRecordRepository;
    private final SubjectAssignmentRepository subjectAssignmentRepository;
    private final StudentAffiliationRepository studentAffiliationRepository;
    private final AcademicCalendarUtil academicCalendarUtil;

    // 담임여부, 학생의 반 소속 여부 검증
    private ValidatedSubjectContext validatedAuthorityAndStudent(
            Long teacherId, Long classroomId, Long subjectId, Long studentId, int year, int semester
    ) {
        SubjectAssignment assignment = subjectAssignmentRepository
                .findValidAssignment(teacherId, classroomId, subjectId, year, semester)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED, "해당 반의 과목 담당 교사가 아닙니다"));

        StudentAffiliation affiliation = studentAffiliationRepository.findWithAllDetails(studentId, year, semester)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_ASSIGNED));

        if (!affiliation.getClassroom().getId().equals(classroomId)) {
            throw new BusinessException(ErrorCode.STUDENT_NOT_IN_CLASSROOM, "해당 학생은 해당 반의 소속이 아닙니다");
        }

        return new ValidatedSubjectContext(assignment, affiliation);
    }

    // 과세특 상세 조회
    @Transactional(readOnly = true)
    public SubjectRecordResponse getSubjectRecord(Long classroomId, Long studentId, Long subjectId, Long teacherId) {
        int currentYear = academicCalendarUtil.getCurrentAcademicYear();
        int currentSemester = academicCalendarUtil.getCurrentSemester();

        validatedAuthorityAndStudent(teacherId, classroomId, subjectId, studentId, currentYear, currentSemester);
        boolean canEdit = academicCalendarUtil.isModifiable(currentYear);

        return studentRecordRepository.findByStudentIdAndRecordCategoryAndSubjectIdAndAcademicYearAndSemester(
                studentId, RecordCategory.SUBJECT_OPINION, subjectId, currentYear, currentSemester)
                .map(record -> SubjectRecordResponse.builder()
                        .recordId(record.getId())
                        .content(record.getContent())
                        .canEdit(canEdit)
                        .build())
                .orElseGet(() -> SubjectRecordResponse.builder()
                        .recordId(null)
                        .content("")
                        .canEdit(canEdit)
                        .build());
    }

    // 과세특 저장/수정
    @Transactional
    public void saveSubjectRecord(Long classroomId, Long studentId, Long subjectId, Long teacherId, SubjectRecordRequest request) {
        int currentYear = academicCalendarUtil.getCurrentAcademicYear();
        int currentSemester = academicCalendarUtil.getCurrentSemester();

        if (!academicCalendarUtil.isModifiable(currentYear)) {
            throw new BusinessException(ErrorCode.RECORD_DEADLINE_EXCEEDED,
                    "해당 학년도의 과세특 작성 및 수정 기간은 마감되었습니다");
        }

        ValidatedSubjectContext context = validatedAuthorityAndStudent(
                teacherId, classroomId, subjectId, studentId, currentYear, currentSemester);

        studentRecordRepository.findByStudentIdAndRecordCategoryAndSubjectIdAndAcademicYearAndSemester(
                studentId, RecordCategory.SUBJECT_OPINION, subjectId, currentYear, currentSemester)
                .ifPresentOrElse(
                        existingRecord -> existingRecord.updateContent(
                                request.getContent(),
                                context.assignment().getTeacher()
                        ),
                        () -> {
                            StudentRecord newRecord = StudentRecord.createSubjectOpinion(
                                    context.affiliation().getStudent(),
                                    context.assignment().getTeacher(),
                                    currentYear,
                                    currentSemester,
                                    context.assignment().getSubject(),
                                    request.getContent()
                            );
                            studentRecordRepository.save(newRecord);
                        }
                );
    }

    // 내부 전용 Record
    private record ValidatedSubjectContext(SubjectAssignment assignment, StudentAffiliation affiliation) {}
}
