package com.school.studentmanagement.parent.service;

import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.global.util.AcademicCalendarUtil;
import com.school.studentmanagement.parent.dto.ChildInfoResponse;
import com.school.studentmanagement.parent.entity.ParentStudentMapping;
import com.school.studentmanagement.parent.repository.ParentStudentMappingRepository;
import com.school.studentmanagement.student.dto.StudentMyGradeResponse;
import com.school.studentmanagement.student.dto.StudentMyRecordResponse;
import com.school.studentmanagement.student.entity.Student;
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
        validateLinked(parentId, studentId);
        return studentMyPageService.getMyGrades(studentId, academicYear, semester);
    }

    public StudentMyRecordResponse getChildRecords(Long parentId, Long studentId, Integer academicYear, Integer semester) {
        validateLinked(parentId, studentId);
        return studentMyPageService.getMyRecords(studentId, academicYear, semester);
    }

    private void validateLinked(Long parentId, Long studentId) {
        if (!mappingRepository.existsByParentIdAndStudentId(parentId, studentId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "연결된 자녀가 아닙니다");
        }
    }
}
