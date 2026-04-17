package com.school.studentmanagement.classroom.service;

import com.school.studentmanagement.StudentAffiliation.entity.StudentAffiliation;
import com.school.studentmanagement.StudentAffiliation.repository.StudentAffiliationRepository;
import com.school.studentmanagement.classroom.dto.StudentListResponse;
import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.classroom.repository.ClassRoomRepository;
import com.school.studentmanagement.global.util.AcademicCalendarUtil;
import com.school.studentmanagement.subject.repository.SubjectAssignmentRepository;
import com.school.studentmanagement.subject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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

        // 서버 시간을 기존으로 현재 연도/학기 고정
        int currentYear = academicCalendarUtil.getCurrentAcademicYear();
        int currentSemester = academicCalendarUtil.getCurrentSemester();

        // 선생님이 담임인 반 찾기
        Classroom homeroom = classRoomRepository.findClassroomByHomeroomTeacherIdAndAcademicYearAndSemester(
                teacherId, currentYear, currentSemester)
                .orElseThrow(() -> new IllegalArgumentException(currentYear + "학년도" + currentSemester + "학기에는 담임을 담당한 반이 없습니다"));

        // 반에 소속된 학생 정보를 번호순으로 가져온다
        var affiliations = studentAffiliationRepository.findAllByClassroomId(homeroom.getId());

        // DTO 리스트로 리턴
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

        // 선생님이 해당 반에 들어가는 담당 선생님이 맞는지 검사
        boolean hasAccess = subjectAssignmentRepository.existsByTeacherIdAndClassroomIdAndYear(teacherId, classroomId, currentYear, currentSemester);
        if (!hasAccess) {
            throw new AccessDeniedException("해당 반 수업의 담당 교사가 아닙니다");
        }

        // DB에서 학생 리스트 가져오기
        List<StudentAffiliation> affiliations = studentAffiliationRepository.findAllByClassroomId(classroomId);

        // Entity -> DTO 변환 후 리턴
        return affiliations.stream()
                .map(aff -> StudentListResponse.builder()
                        .studentId(aff.getStudent().getId())
                        .studentNum(aff.getStudentNum())
                        .name(aff.getStudent().getUser().getName())
                        .build())
                .collect(Collectors.toList());
    }
}
