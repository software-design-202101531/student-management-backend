package com.school.studentmanagement.record.service;

import com.school.studentmanagement.StudentAffiliation.entity.StudentAffiliation;
import com.school.studentmanagement.StudentAffiliation.repository.StudentAffiliationRepository;
import com.school.studentmanagement.global.enums.RecordCategory;
import com.school.studentmanagement.global.util.AcademicCalendarUtil;
import com.school.studentmanagement.record.dto.BehaviorRecordRequest;
import com.school.studentmanagement.record.dto.BehaviorRecordResponse;
import com.school.studentmanagement.record.entity.StudentRecord;
import com.school.studentmanagement.record.repository.StudentRecordRepository;
import com.school.studentmanagement.user.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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
        // 해당 연도/학기에 배정된 학생의 반 이력 탐색
        StudentAffiliation affiliation = studentAffiliationRepository.findWithAllDetails(studentId, year, semester)
                .orElseThrow(() -> new IllegalStateException("해당 학기에 배정된 학생이 아닙니다"));

        // 담임 선생님이 맞는지 확인
        if (!affiliation.getClassroom().getHomeroomTeacher().getId().equals(teacherId)) {
            throw new AccessDeniedException("담당 학생이 아니기에 접근할 수 없습니다");
        }

        return affiliation;
    }

    // 기존 행특 정보 조회
    @Transactional(readOnly = true)
    public BehaviorRecordResponse getBehaviorRecord(Long studentId, Long teacherId) {
        int currentYear = academicCalendarUtil.getCurrentAcademicYear();
        int currentSemester = academicCalendarUtil.getCurrentSemester();

        validateHomeroomTeacherAuthority(studentId, teacherId, currentYear, currentSemester); // 값을 필요로 하는 것이 아닌 단순 검증용도
        boolean canEdit = academicCalendarUtil.isModifiable(currentYear);

        return studentRecordRepository.findByStudentIdAndRecordCategoryAndAcademicYearAndSemester(
                studentId, RecordCategory.BEHAVIOR_OPINION, currentYear, currentSemester)
                .map(record -> BehaviorRecordResponse.builder()
                        .recordId(record.getId())
                        .content((record.getContent()))
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

        // 마감 기한 체크
        if (!academicCalendarUtil.isModifiable(currentYear)) {
            throw new IllegalArgumentException("해당 학년도의 행특 작성 및 수정 기간은 마감되었습니다");
        }

        StudentAffiliation affiliation = validateHomeroomTeacherAuthority(studentId, teacherId, currentYear, currentSemester);


        studentRecordRepository.findByStudentIdAndRecordCategoryAndAcademicYearAndSemester(
                studentId, RecordCategory.BEHAVIOR_OPINION, currentYear, currentSemester)
                .ifPresentOrElse(
                        // Case 1: 덮어쓰기(Update)
                        existingRecord -> existingRecord.updateContent(
                                request.getContent(),
                                teacherRepository.getReferenceById(teacherId)
                        ),

                        // case 2: 새로 만들기
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
