package com.school.studentmanagement.record.service;

import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.global.enums.RecordCategory;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.global.util.AcademicCalendarUtil;
import com.school.studentmanagement.record.dto.BehaviorRecordRequest;
import com.school.studentmanagement.record.dto.BehaviorRecordResponse;
import com.school.studentmanagement.record.entity.StudentRecord;
import com.school.studentmanagement.record.repository.StudentRecordRepository;
import com.school.studentmanagement.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentRecordService {

    private final StudentRecordRepository studentRecordRepository;
    private final StudentAffiliationRepository studentAffiliationRepository;
    private final AcademicCalendarUtil academicCalendarUtil;
    private final TeacherRepository teacherRepository;

    // 올해/이번 학기 이 학생의 담임이 맞는지 권한 확인
    private StudentAffiliation validateHomeroomTeacherAuthority(Long studentId, Long teacherId, int year, int semester) {
        StudentAffiliation affiliation = studentAffiliationRepository.findWithAllDetails(studentId, year, semester)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_ASSIGNED));

        if (!affiliation.getClassroom().getHomeroomTeacher().getId().equals(teacherId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "담당 학생이 아니기에 접근할 수 없습니다");
        }

        return affiliation;
    }

    // 기존 행특 정보 조회
    @Transactional(readOnly = true)
    public BehaviorRecordResponse getBehaviorRecord(Long studentId, Long teacherId) {
        int currentYear = academicCalendarUtil.getCurrentAcademicYear();
        int currentSemester = academicCalendarUtil.getCurrentSemester();

        validateHomeroomTeacherAuthority(studentId, teacherId, currentYear, currentSemester);
        boolean canEdit = academicCalendarUtil.isModifiable(currentYear);

        return studentRecordRepository.findByStudentIdAndRecordCategoryAndAcademicYearAndSemester(
                studentId, RecordCategory.BEHAVIOR_OPINION, currentYear, currentSemester)
                .map(record -> BehaviorRecordResponse.builder()
                        .recordId(record.getId())
                        .content(record.getContent())
                        .canEdit(canEdit)
                        .build())
                .orElseGet(() -> BehaviorRecordResponse.builder()
                        .recordId(null)
                        .content("")
                        .canEdit(canEdit)
                        .build());
    }

    // 행특 저장 및 수정(POST, PUT)
    @Transactional
    public void saveBehaviorRecord(Long studentId, Long teacherId, BehaviorRecordRequest request) {
        int currentYear = academicCalendarUtil.getCurrentAcademicYear();
        int currentSemester = academicCalendarUtil.getCurrentSemester();

        if (!academicCalendarUtil.isModifiable(currentYear)) {
            throw new BusinessException(ErrorCode.RECORD_DEADLINE_EXCEEDED,
                    "해당 학년도의 행특 작성 및 수정 기간은 마감되었습니다");
        }

        StudentAffiliation affiliation = validateHomeroomTeacherAuthority(studentId, teacherId, currentYear, currentSemester);

        studentRecordRepository.findByStudentIdAndRecordCategoryAndAcademicYearAndSemester(
                studentId, RecordCategory.BEHAVIOR_OPINION, currentYear, currentSemester)
                .ifPresentOrElse(
                        existingRecord -> existingRecord.updateContent(
                                request.getContent(),
                                teacherRepository.getReferenceById(teacherId)
                        ),
                        () -> {
                            StudentRecord newRecord = StudentRecord.createBehaviorOpinion(
                                    affiliation.getStudent(),
                                    teacherRepository.getReferenceById(teacherId),
                                    currentYear,
                                    currentSemester,
                                    request.getContent()
                            );
                            studentRecordRepository.save(newRecord);
                        }
                );
    }
}
