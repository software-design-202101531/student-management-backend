package com.school.studentmanagement.global.validation;

import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.global.util.AcademicCalendarUtil;
import com.school.studentmanagement.subject.repository.SubjectAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class TeacherStudentRelationValidator {

    private final StudentAffiliationRepository studentAffiliationRepository;
    private final SubjectAssignmentRepository subjectAssignmentRepository;
    private final AcademicCalendarUtil academicCalendarUtil;

    // 담임, 혹은 과제 담당 선생님인지 확인 메서드
    public void validateCanWriteFor(Long teacherId, Long studentId) {
        int year = academicCalendarUtil.getCurrentAcademicYear();
        int semester = academicCalendarUtil.getCurrentSemester();

        StudentAffiliation affiliation = studentAffiliationRepository
                .findWithAllDetails(studentId, year, semester)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_ASSIGNED));

        Long classroomId = affiliation.getClassroom().getId();

        // 담임 선생님인지 확인
        boolean isHomeroom = affiliation.getClassroom().getHomeroomTeacher() != null
                && affiliation.getClassroom().getHomeroomTeacher().getId().equals(teacherId);
        if (isHomeroom) {
            return;
        }

        // 과목 당당 선생님인지 확인
        boolean isSubjectTeacher = subjectAssignmentRepository
                .existsByTeacherIdAndClassroomIdAndAcademicYearAndSemester(
                        teacherId, classroomId, year, semester);
        if (isSubjectTeacher) {
            return;
        }

        throw new BusinessException(ErrorCode.ACCESS_DENIED,
                "해당 학생에 대한 작성 권한이 없습니다(담임 또는 과목 담당 교사만 가능)");
    }
}
