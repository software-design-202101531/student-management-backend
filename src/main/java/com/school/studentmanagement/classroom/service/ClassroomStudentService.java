package com.school.studentmanagement.classroom.service;

import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.classroom.dto.StudentListResponse;
import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.classroom.repository.ClassRoomRepository;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.global.util.AcademicCalendarUtil;
import com.school.studentmanagement.subject.repository.SubjectAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassroomStudentService {

    private final ClassRoomRepository classRoomRepository;
    private final StudentAffiliationRepository studentAffiliationRepository;
    private final AcademicCalendarUtil academicCalendarUtil;
    private final SubjectAssignmentRepository subjectAssignmentRepository;

    // 현재 연도/학기 기준으로 로그인 한 선생님의 담임 반 학생 목록을 조회
    @Transactional(readOnly = true)
    public List<StudentListResponse> getMyHomeroomStudents(Long teacherId) {
        int currentYear = academicCalendarUtil.getCurrentAcademicYear();
        int currentSemester = academicCalendarUtil.getCurrentSemester();

        Classroom homeroom = classRoomRepository
                .findClassroomByHomeroomTeacherIdAndAcademicYearAndSemester(teacherId, currentYear, currentSemester)
                .orElseThrow(() -> new BusinessException(ErrorCode.HOMEROOM_NOT_FOUND,
                        currentYear + "학년도 " + currentSemester + "학기에는 담임을 담당한 반이 없습니다"));

        var affiliations = studentAffiliationRepository.findAllByClassroomId(homeroom.getId());

        return affiliations.stream()
                .map(aff -> StudentListResponse.builder()
                        .studentId(aff.getStudent().getId())
                        .studentNum(aff.getStudentNum())
                        .name(aff.getStudent().getUser().getName())
                        .build())
                .collect(Collectors.toList());
    }

    // 수업 담당 반 중 하나의 반의 학생 리스트를 조회
    @Transactional(readOnly = true)
    public List<StudentListResponse> getStudentsInClassroom(Long classroomId, Long teacherId) {
        int currentYear = academicCalendarUtil.getCurrentAcademicYear();
        int currentSemester = academicCalendarUtil.getCurrentSemester();

        boolean hasAccess = subjectAssignmentRepository
                .existsByTeacherIdAndClassroomIdAndYear(teacherId, classroomId, currentYear, currentSemester);
        if (!hasAccess) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "해당 반 수업의 담당 교사가 아닙니다");
        }

        List<StudentAffiliation> affiliations = studentAffiliationRepository.findAllByClassroomId(classroomId);

        return affiliations.stream()
                .map(aff -> StudentListResponse.builder()
                        .studentId(aff.getStudent().getId())
                        .studentNum(aff.getStudentNum())
                        .name(aff.getStudent().getUser().getName())
                        .build())
                .collect(Collectors.toList());
    }
}
