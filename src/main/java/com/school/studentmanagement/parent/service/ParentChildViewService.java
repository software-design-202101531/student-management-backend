package com.school.studentmanagement.parent.service;

import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.global.util.AcademicCalendarUtil;
import com.school.studentmanagement.parent.dto.ChildInfoResponse;
import com.school.studentmanagement.parent.entity.ParentStudentMapping;
import com.school.studentmanagement.parent.repository.ParentStudentMappingRepository;
import com.school.studentmanagement.parent.validator.ParentChildLinkValidator;
import com.school.studentmanagement.student.dto.StudentMyAttendanceResponse;
import com.school.studentmanagement.student.dto.StudentMyGradeResponse;
import com.school.studentmanagement.student.dto.StudentMyRecordResponse;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.student.service.StudentAttendanceService;
import com.school.studentmanagement.student.service.StudentMyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParentChildViewService {

    private final ParentStudentMappingRepository mappingRepository;
    private final StudentAffiliationRepository affiliationRepository;
    private final AcademicCalendarUtil academicCalendarUtil;
    private final StudentMyPageService studentMyPageService;
    private final StudentAttendanceService studentAttendanceService;
    private final ParentChildLinkValidator parentChildLinkValidator;

    public List<ChildInfoResponse> getMyChildren(Long parentId) {
        int year = academicCalendarUtil.getCurrentAcademicYear();
        int sem  = academicCalendarUtil.getCurrentSemester();

        List<ParentStudentMapping> mappings = mappingRepository.findAllByParentIdWithStudent(parentId);

        return mappings.stream()
                .map(mapping -> {
                    Student student = mapping.getStudent();
                    StudentAffiliation aff = affiliationRepository
                            .findWithAllDetails(student.getId(), year, sem)
                            .orElse(null);

                    return ChildInfoResponse.builder()
                            .studentId(student.getId())
                            .name(student.getUser().getName())
                            .academicYear(year)
                            .semester(sem)
                            .grade(aff != null ? aff.getClassroom().getGrade() : null)
                            .classNum(aff != null ? aff.getClassroom().getClassNum() : null)
                            .studentNum(aff != null ? aff.getStudentNum() : null)
                            .build();
                })
                .toList();
    }

    public StudentMyGradeResponse getChildGrades(Long parentId, Long studentId, Integer academicYear, Integer semester) {
        parentChildLinkValidator.validateLinked(parentId, studentId);
        return studentMyPageService.getMyGrades(studentId, academicYear, semester);
    }

    public StudentMyRecordResponse getChildRecords(Long parentId, Long studentId, Integer academicYear, Integer semester) {
        parentChildLinkValidator.validateLinked(parentId, studentId);
        return studentMyPageService.getMyRecords(studentId, academicYear, semester);
    }

    public StudentMyAttendanceResponse getChildMonthlyAttendance(Long parentId, Long studentId, int year, int month) {
        parentChildLinkValidator.validateLinked(parentId, studentId);
        return studentAttendanceService.getMyMonthlyAttendance(studentId, year, month);
    }
}
