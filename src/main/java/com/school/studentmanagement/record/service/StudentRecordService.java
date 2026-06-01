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
import com.school.studentmanagement.teacher.entity.Teacher;
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

        // 담임 미배정 학급(homeroomTeacher == null)일 수 있으므로 null 안전하게 검사한다
        Teacher homeroomTeacher = affiliation.getClassroom().getHomeroomTeacher();
        if (homeroomTeacher == null || !homeroomTeacher.getId().equals(teacherId)) {
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

        validateHomeroomTeacherAuthority(studentId, teacherId, currentYear, currentSemester);

        // 기존 행특이 있으면 수정 — @Version 낙관적 락이 동시 수정 시 갱신 손실을 탐지해 409로 거부한다.
        var existing = studentRecordRepository.findByStudentIdAndRecordCategoryAndAcademicYearAndSemester(
                studentId, RecordCategory.BEHAVIOR_OPINION, currentYear, currentSemester);
        if (existing.isPresent()) {
            existing.get().updateContent(request.getContent(), teacherRepository.getReferenceById(teacherId));
            return;
        }

        // 미존재 → 동시 최초 작성 경합에 안전한 삽입.
        // ON CONFLICT DO NOTHING 은 예외를 던지지 않아 rollback-only 함정을 피한다(부분 유니크 인덱스가 전제).
        int inserted = studentRecordRepository.insertBehaviorIfAbsent(
                studentId, teacherId, currentYear, currentSemester, request.getContent());
        if (inserted == 0) {
            // 다른 교사가 한발 먼저 생성 → 이미 커밋된 행을 재조회해 일반 update 경로(@Version)로 수렴한다.
            StudentRecord winner = studentRecordRepository
                    .findByStudentIdAndRecordCategoryAndAcademicYearAndSemester(
                            studentId, RecordCategory.BEHAVIOR_OPINION, currentYear, currentSemester)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RECORD_CONFLICT));
            winner.updateContent(request.getContent(), teacherRepository.getReferenceById(teacherId));
        }
    }
}
