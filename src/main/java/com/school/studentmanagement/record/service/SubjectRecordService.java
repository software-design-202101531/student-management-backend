package com.school.studentmanagement.record.service;

import com.school.studentmanagement.StudentAffiliation.entity.StudentAffiliation;
import com.school.studentmanagement.StudentAffiliation.repository.StudentAffiliationRepository;
import com.school.studentmanagement.global.enums.RecordCategory;
import com.school.studentmanagement.global.util.AcademicCalendarUtil;
import com.school.studentmanagement.record.entity.StudentRecord;
import com.school.studentmanagement.record.repository.StudentRecordRepository;
import com.school.studentmanagement.subject.dto.SubjectRecordRequest;
import com.school.studentmanagement.subject.dto.SubjectRecordResponse;
import com.school.studentmanagement.subject.entity.SubjectAssignment;
import com.school.studentmanagement.subject.repository.SubjectAssignmentRepository;
import com.school.studentmanagement.user.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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
            Long teacherId,
            Long classroomId,
            Long subjectId,
            Long studentId,
            int year, int semester
    ) {
        // 과목 배정 테이블을 조회하여 권함 검사
        SubjectAssignment assignment = subjectAssignmentRepository.findValidAssignment(teacherId, classroomId, subjectId, year, semester)
                .orElseThrow(() -> new AccessDeniedException("해당 반의 과목 담당 교사가 아닙니다"));

        // 학생 배정 테이블을 조회하여 소속 검사
        StudentAffiliation affiliation = studentAffiliationRepository.findWithAllDetails(studentId, year, semester)
                .orElseThrow(() -> new IllegalArgumentException("해당 학기에 배정된 학생 이력을 확인할 수 없습니다"));

        // 찾은 학생이 해당 반 소속이 맞는지 검사
        if (!affiliation.getClassroom().getId().equals(classroomId)) {
            throw new AccessDeniedException("해당 학생은 해당 반의 소속이 아닙니다");
        }

        return new ValidatedSubjectContext(assignment, affiliation);
    }

    // 과세특 상세 조회
    @Transactional(readOnly = true)
    public SubjectRecordResponse getSubjectRecord(Long classroomId, Long studentId, Long subjectId, Long teacherId) {
        int currentYear = academicCalendarUtil.getCurrentAcademicYear();
        int currentSemester = academicCalendarUtil.getCurrentSemester();

        // 권한검증
        validatedAuthorityAndStudent(teacherId, classroomId, subjectId, studentId, currentYear, currentSemester);
        boolean canEdit = academicCalendarUtil.isModifiable(currentYear);

        return studentRecordRepository.findByStudentIdAndRecordCategoryAndSubjectIdAndAcademicYearAndSemester(
                studentId,
                RecordCategory.SUBJECT_OPINION,
                subjectId,
                currentYear,
                currentSemester)
                .map(record -> SubjectRecordResponse.builder() // 기존 데이터가 있다면 매핑
                        .recordId(record.getId())
                        .content(record.getContent())
                        .canEdit(canEdit)
                        .build())
                .orElseGet(() -> SubjectRecordResponse.builder() // 기존 데이터가 없다면 빈칸으로 생성
                        .recordId(null)
                        .content("")
                        .canEdit(canEdit)
                        .build());
    }

    // 과세특 저장/수정
    @Transactional
    public void saveSubjectRecord(Long classroomId, Long studentId, Long subjectId, Long teacherId, SubjectRecordRequest request) {
        // 서버 시간을 기준으로 현재 연도, 학기를 파악
        int currentYear = academicCalendarUtil.getCurrentAcademicYear();
        int currentSemester = academicCalendarUtil.getCurrentSemester();

        // 수정 기간 검증
        if (!academicCalendarUtil.isModifiable(currentYear)) {
            throw new IllegalStateException("해당 학년도의 과세특 작성 및 수정 기간은 마감되었습니다");
        }

        // 검증 및 데이터(과목, 학생) 획득
        ValidatedSubjectContext context = validatedAuthorityAndStudent(teacherId, classroomId, subjectId, studentId, currentYear, currentSemester);

        studentRecordRepository.findByStudentIdAndRecordCategoryAndSubjectIdAndAcademicYearAndSemester(
                studentId, RecordCategory.SUBJECT_OPINION, subjectId, currentYear, currentSemester)
                .ifPresentOrElse(
                        // Case 1: 덮어쓰기(Update)
                        existingRecord -> existingRecord.updateContent(
                                request.getContent(),
                                context.assignment().getTeacher()
                        ),
                        // Case 2: 새로 만들기
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
