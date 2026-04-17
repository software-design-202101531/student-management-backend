package com.school.studentmanagement.attendance.service;

import com.school.studentmanagement.StudentAffiliation.entity.StudentAffiliation;
import com.school.studentmanagement.StudentAffiliation.repository.StudentAffiliationRepository;
import com.school.studentmanagement.attendance.dto.AttendanceDailyResponse;
import com.school.studentmanagement.attendance.dto.AttendanceMonthlyResponse;
import com.school.studentmanagement.attendance.dto.AttendanceSaveRequest;
import com.school.studentmanagement.attendance.entity.Attendance;
import com.school.studentmanagement.attendance.repository.AttendanceRepository;
import com.school.studentmanagement.calendar.entity.AcademicCalendar;
import com.school.studentmanagement.calendar.repository.AcademicCalendarRepository;
import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.classroom.repository.ClassRoomRepository;
import com.school.studentmanagement.global.enums.AttendanceStatus;
import com.school.studentmanagement.user.entity.Teacher;
import com.school.studentmanagement.user.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final ClassRoomRepository classRoomRepository;
    private final AcademicCalendarRepository academicCalendarRepository;
    private final StudentAffiliationRepository studentAffiliationRepository;
    private final AttendanceRepository attendanceRepository;
    private final TeacherRepository teacherRepository;

    // 담임 선생님이 맞는지 검증
    private void validateHomeroomTeacher(Long classroomId, Long teacherId) {
        Classroom classroom = classRoomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("반 정보가 존재하지 않습니다"));

        if(!classroom.getHomeroomTeacher().getId().equals(teacherId)) {
            throw new AccessDeniedException("해당 반의 담임이 아닙니다");
        }
    }

    // 월간 달력 조회
    @Transactional(readOnly = true)
    public AttendanceMonthlyResponse getMonthlyAttendance(Long classroomId, Long teacherId, int year, int month) {
        validateHomeroomTeacher(classroomId, teacherId);

        // 휴일 리스트 가져오기
        List<AcademicCalendar> holidays = academicCalendarRepository.findHolidaysByYearAndMonth(year, month);

        // Entity -> DTO 변환
        List<AttendanceMonthlyResponse.HolidayDto> holidayDtos = holidays.stream()
                .map(h -> AttendanceMonthlyResponse.HolidayDto.builder()
                        .date(h.getDate())
                        .name(h.getDescription())
                        .build())
                .collect(Collectors.toList());

        return AttendanceMonthlyResponse.builder()
                .year(year)
                .month(month)
                .holidays(holidayDtos)
                .build();
    }

    // 일간 상세 조회
    @Transactional(readOnly = true)
    public AttendanceDailyResponse getDailyAttendance(Long classroomId, Long teacherId, LocalDate date) {

        // 담임 선생님이 맞는지 검증
        validateHomeroomTeacher(classroomId, teacherId);

        // 휴일 여부 확인
        AcademicCalendar calendar = academicCalendarRepository.findByDate(date).orElse(null);
        boolean isHoliday = calendar != null && !calendar.getDayType().name().equals("WEEKDAY");

        // 반 학생 명단 가져오기
        List<StudentAffiliation> affiliations = studentAffiliationRepository.findAllByClassroomId(classroomId);
        List<Long> studentIds = affiliations.stream().map(a -> a.getStudent().getId()).collect(Collectors.toList());

        // 특이사항(결석/지각) 데이터 가져오기
        List<Attendance> existingRecords = attendanceRepository.findByStudentIdsAndDate(studentIds, date);

        // 학생 ID를 Key로 가지는 Map으로 변환(빠른 탐색을 위해서)
        Map<Long, Attendance> recordMap = existingRecords.stream()
                .collect(Collectors.toMap(a -> a.getStudent().getId(), a -> a));

        // DTO로 변환(학생 데이터와 출결 데이터를 하나의 형태로 변환)
        List<AttendanceDailyResponse.StudentAttendanceDto> studentDtos = affiliations.stream().map(aff -> {
            // 학생 ID를 기반으로 출석 기록 가져오기
            Attendance record = recordMap.get(aff.getStudent().getId());

            // 출석 리스트 만들기
            return AttendanceDailyResponse.StudentAttendanceDto.builder()
                    .studentId(aff.getStudent().getId())
                    .studentNum(aff.getStudentNum())
                    .studentName(aff.getStudent().getUser().getName())
                    // DB의 데이터 여부에 따라 삼항처리
                    .status(record != null ? record.getStatus() : AttendanceStatus.PRESENT) // DB에 없으면 정상
                    .reason(record != null ? record.getReason() : null)
                    .build();
        }).collect(Collectors.toList());

        return AttendanceDailyResponse.builder()
                .date(date)
                .isHoliday(isHoliday)
                .holidayName(isHoliday ? calendar.getDescription() : null)
                .students(studentDtos)
                .build();
    }

    // 수정사항 일괄 저장
    @Transactional
    public void saveDailyAttendance(Long classroomId, Long teacherId, LocalDate date, AttendanceSaveRequest request) {
        // 담인 선생님이 맞는지 검증
        validateHomeroomTeacher(classroomId, teacherId);

        // 서버시간 기준 오늘 이후의 출결 입력 방지
        if(date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("출결은 미리 입력할 수 없습니다");
        }

        Teacher teacher = teacherRepository.getReferenceById(teacherId);
        List<Long> requestStudentIds = request.getAttendanceData().stream()
                .map(AttendanceSaveRequest.StudentAttendanceUpdateDto::getStudentId)
                .toList();

        // 기존에 DB에 있던 기록들 가져오기
        List<Attendance> existingRecords = attendanceRepository.findByStudentIdsAndDate(requestStudentIds, date);
        Map<Long, Attendance> existingMap = existingRecords.stream()
                .collect(Collectors.toMap(a -> a.getStudent().getId(), a -> a));

        // 프론트가 보낸 데이터 처리
        for (AttendanceSaveRequest.StudentAttendanceUpdateDto dto : request.getAttendanceData()) {
            Attendance existing = existingMap.get(dto.getStudentId());

            if (dto.getStatus() == AttendanceStatus.PRESENT) {
                // Case 1: 정상으로 바꾼 후 DB에 기록이 있다면 지우기
                if (existing != null) {
                    attendanceRepository.delete(existing);
                }
            } else {
                if (existing != null) {
                    // Case 2: 이미 지각/결적이었고 이번에도 지각/결석
                    existing.updateStatus(dto.getStatus(), dto.getReason(), teacher);
                } else {
                    // Case 3: 새로 발생한 결석/지각
                    StudentAffiliation affiliation = studentAffiliationRepository.findByStudentIdAndClassroomId(dto.getStudentId(), classroomId)
                            .orElseThrow(() -> new IllegalArgumentException("해당 반의 학생이 아닙니다"));

                    // 상태 업데이트 후 저장
                    Attendance newRecord = Attendance.builder()
                            .student(affiliation.getStudent())
                            .teacher(teacher)
                            .date(date)
                            .status(dto.getStatus())
                            .reason(dto.getReason())
                            .build();
                    attendanceRepository.save(newRecord);
                }
            }
        }
    }
}
