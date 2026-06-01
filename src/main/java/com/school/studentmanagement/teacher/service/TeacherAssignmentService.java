package com.school.studentmanagement.teacher.service;

import com.school.studentmanagement.global.util.AcademicCalendarUtil;
import com.school.studentmanagement.subject.entity.SubjectAssignment;
import com.school.studentmanagement.subject.repository.SubjectAssignmentRepository;
import com.school.studentmanagement.teacher.dto.TeacherAssignmentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherAssignmentService {

    private final SubjectAssignmentRepository subjectAssignmentRepository;
    private final AcademicCalendarUtil academicCalendarUtil;

    public List<TeacherAssignmentResponse> getMyAssignments(Long teacherId) {
        int currentYear = academicCalendarUtil.getCurrentAcademicYear();
        int currentSemester = academicCalendarUtil.getCurrentSemester();

        List<SubjectAssignment> assignments = subjectAssignmentRepository
                .findAllMyAssignments(teacherId, currentYear, currentSemester);

        return assignments.stream()
                .map(assignment -> TeacherAssignmentResponse.builder()
                        .assignmentId(assignment.getId())
                        .classroomId(assignment.getClassroom().getId())
                        .grade(assignment.getClassroom().getGrade())
                        .subjectName(assignment.getSubject().getName())
                        .classNum(assignment.getClassroom().getClassNum())
                        .subjectId(assignment.getSubject().getId())
                        .build())
                .toList();
    }
}
