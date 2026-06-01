package com.school.studentmanagement.attendance.service;

import com.school.studentmanagement.attendance.dto.StudentAttendanceRangeResponse;
import com.school.studentmanagement.attendance.entity.AcademicCalendar;
import com.school.studentmanagement.attendance.entity.Attendance;
import com.school.studentmanagement.attendance.repository.AcademicCalendarRepository;
import com.school.studentmanagement.attendance.repository.AttendanceRepository;
import com.school.studentmanagement.global.enums.AttendanceStatus;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.global.validation.TeacherStudentRelationValidator;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 교사가 특정 학생 한 명의 출결을 기간(from~to) 단위로 조회하기 위한 서비스.
 * 권한은 담임 또는 과목 담당 교사로 제한(피드백/상담 작성과 동일 정책).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherStudentAttendanceService {

    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final AcademicCalendarRepository academicCalendarRepository;
    private final TeacherStudentRelationValidator teacherStudentRelationValidator;

    public StudentAttendanceRangeResponse getStudentAttendanceRange(
            Long teacherId, Long studentId, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "from 은 to 보다 늦을 수 없습니다");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));

        // 담임 또는 과목 담당 교사만 조회 가능
        teacherStudentRelationValidator.validateCanWriteFor(teacherId, studentId);

        List<Attendance> records = attendanceRepository
                .findByStudentIdAndDateBetween(studentId, from, to);
        List<AcademicCalendar> holidays = academicCalendarRepository
                .findHolidaysByDateBetween(from, to);

        int absent = 0, late = 0, earlyLeave = 0;
        for (Attendance a : records) {
            switch (a.getStatus()) {
                case ABSENT -> absent++;
                case LATE -> late++;
                case EARLY_LEAVE -> earlyLeave++;
                case PRESENT -> { /* PRESENT 는 row 가 없는 게 정상이지만 안전 차원 */ }
            }
        }

        List<StudentAttendanceRangeResponse.Entry> entries = records.stream()
                .map(a -> StudentAttendanceRangeResponse.Entry.builder()
                        .date(a.getDate())
                        .status(a.getStatus())
                        .reason(a.getReason())
                        .build())
                .toList();

        List<StudentAttendanceRangeResponse.Holiday> holidayDtos = holidays.stream()
                .map(h -> StudentAttendanceRangeResponse.Holiday.builder()
                        .date(h.getDate())
                        .name(h.getDescription())
                        .build())
                .toList();

        return StudentAttendanceRangeResponse.builder()
                .studentId(studentId)
                .studentName(student.getUser().getName())
                .from(from)
                .to(to)
                .summary(StudentAttendanceRangeResponse.Summary.builder()
                        .absentDays(absent).lateDays(late).earlyLeaveDays(earlyLeave).build())
                .entries(entries)
                .holidays(holidayDtos)
                .build();
    }
}
