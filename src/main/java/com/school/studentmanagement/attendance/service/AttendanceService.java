package com.school.studentmanagement.attendance.service;

import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.attendance.dto.AttendanceDailyResponse;
import com.school.studentmanagement.attendance.dto.AttendanceMonthlyResponse;
import com.school.studentmanagement.attendance.dto.AttendanceSaveRequest;
import com.school.studentmanagement.attendance.entity.Attendance;
import com.school.studentmanagement.attendance.repository.AttendanceRepository;
import com.school.studentmanagement.attendance.entity.AcademicCalendar;
import com.school.studentmanagement.attendance.repository.AcademicCalendarRepository;
import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.classroom.repository.ClassRoomRepository;
import com.school.studentmanagement.global.enums.AttendanceStatus;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.teacher.entity.Teacher;
import com.school.studentmanagement.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
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
                .orElseThrow(() -> new BusinessException(ErrorCode.CLASSROOM_NOT_FOUND));

        if (!classroom.getHomeroomTeacher().getId().equals(teacherId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "해당 반의 담임이 아닙니다");
        }
    }

    // 월간 달력 조회
    @Transactional(readOnly = true)
    public AttendanceMonthlyResponse getMonthlyAttendance(Long classroomId, Long teacherId, int year, int month) {
        validateHomeroomTeacher(classroomId, teacherId);

        List<AcademicCalendar> holidays = academicCalendarRepository.findHolidaysByYearAndMonth(year, month);

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
        validateHomeroomTeacher(classroomId, teacherId);

        AcademicCalendar calendar = academicCalendarRepository.findByDate(date).orElse(null);
        boolean isHoliday = calendar != null && !calendar.getDayType().name().equals("WEEKDAY");

        List<StudentAffiliation> affiliations = studentAffiliationRepository.findAllByClassroomId(classroomId);
        List<Long> studentIds = affiliations.stream().map(a -> a.getStudent().getId()).collect(Collectors.toList());

        List<Attendance> existingRecords = attendanceRepository.findByStudentIdsAndDate(studentIds, date);

        Map<Long, Attendance> recordMap = existingRecords.stream()
                .collect(Collectors.toMap(a -> a.getStudent().getId(), a -> a));

        List<AttendanceDailyResponse.StudentAttendanceDto> studentDtos = affiliations.stream().map(aff -> {
            Attendance record = recordMap.get(aff.getStudent().getId());
            return AttendanceDailyResponse.StudentAttendanceDto.builder()
                    .studentId(aff.getStudent().getId())
                    .studentNum(aff.getStudentNum())
                    .studentName(aff.getStudent().getUser().getName())
                    .status(record != null ? record.getStatus() : AttendanceStatus.PRESENT)
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
        validateHomeroomTeacher(classroomId, teacherId);

        if (date.isAfter(LocalDate.now())) {
            throw new BusinessException(ErrorCode.ATTENDANCE_FUTURE_DATE);
        }

        Teacher teacher = teacherRepository.getReferenceById(teacherId);
        List<Long> requestStudentIds = request.getAttendanceData().stream()
                .map(AttendanceSaveRequest.StudentAttendanceUpdateDto::getStudentId)
                .toList();

        List<Attendance> existingRecords = attendanceRepository.findByStudentIdsAndDate(requestStudentIds, date);
        Map<Long, Attendance> existingMap = existingRecords.stream()
                .collect(Collectors.toMap(a -> a.getStudent().getId(), a -> a));

        for (AttendanceSaveRequest.StudentAttendanceUpdateDto dto : request.getAttendanceData()) {
            Attendance existing = existingMap.get(dto.getStudentId());

            if (dto.getStatus() == AttendanceStatus.PRESENT) {
                if (existing != null) {
                    attendanceRepository.delete(existing);
                }
            } else {
                if (existing != null) {
                    existing.updateStatus(dto.getStatus(), dto.getReason(), teacher);
                } else {
                    StudentAffiliation affiliation = studentAffiliationRepository
                            .findByStudentIdAndClassroomId(dto.getStudentId(), classroomId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_IN_CLASSROOM));

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
