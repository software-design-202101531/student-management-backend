package com.school.studentmanagement.subject.service;

import com.school.studentmanagement.global.util.AcademicCalendarUtil;
import com.school.studentmanagement.subject.dto.TeacherAssignmentResponse;
import com.school.studentmanagement.subject.entity.SubjectAssignment;
import com.school.studentmanagement.subject.repository.SubjectAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherAssignmentService {

    private final SubjectAssignmentRepository subjectAssignmentRepository;
    private final AcademicCalendarUtil academicCalendarUtil;

    // 현재 시점을 기준으로 내 담당 반(수업 기준) 리스트를 조회하기
    @Transactional(readOnly = true)
    public List<TeacherAssignmentResponse> getMyAssignments(Long teacherId) {
        // 현재 학사 연도 및 학기 조회
        int currentYear = academicCalendarUtil.getCurrentAcademicYear();
        int currentSemester = academicCalendarUtil.getCurrentSemester();

        // 현재 연도, 학기의 배정 반 불러오기
        List<SubjectAssignment> assignments = subjectAssignmentRepository.findAllMyAssignments(teacherId, currentYear, currentSemester);

        // Entity -> DTO 변환 후 반환
        return assignments.stream()
                .map(assignment -> TeacherAssignmentResponse.builder()
                        .assignmentId(assignment.getId())
                        .classroomId(assignment.getClassroom().getId())
                        .grade(assignment.getClassroom().getGrade())
                        .subjectName(assignment.getSubject().getName())
                        .classNum(assignment.getClassroom().getClassNum())
                        .subjectId(assignment.getSubject().getId())
                        .build())
                .collect(Collectors.toList());

    }
}
