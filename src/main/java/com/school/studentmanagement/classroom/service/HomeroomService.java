package com.school.studentmanagement.classroom.service;

import com.school.studentmanagement.StudentAffiliation.repository.StudentAffiliationRepository;
import com.school.studentmanagement.classroom.dto.HomeroomStudentResponse;
import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.classroom.repository.ClassRoomRepository;
import com.school.studentmanagement.global.util.AcademicCalendarUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeroomService {

    private final ClassRoomRepository classRoomRepository;
    private final StudentAffiliationRepository studentAffiliationRepository;
    private final AcademicCalendarUtil academicCalendarUtil;

    // 현재 연도/학기 기준으로 로그인 한 선생님의 담임 반 학생 목록을 조회
    @Transactional(readOnly = true)
    public List<HomeroomStudentResponse> getMyHomeroomStudents(Long teacherId) {

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
                .map(aff -> HomeroomStudentResponse.builder()
                        .studentId(aff.getStudent().getId())
                        .studentNum(aff.getStudentNum())
                        .name(aff.getStudent().getUser().getName())
                        .gender(aff.getStudent().getUser().getGender().name())
                        .build())
                .collect(Collectors.toList());
    }
}
