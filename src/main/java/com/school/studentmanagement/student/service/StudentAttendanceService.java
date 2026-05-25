package com.school.studentmanagement.student.service;

import com.school.studentmanagement.attendance.entity.AcademicCalendar;
import com.school.studentmanagement.attendance.entity.Attendance;
import com.school.studentmanagement.attendance.repository.AcademicCalendarRepository;
import com.school.studentmanagement.attendance.repository.AttendanceRepository;
import com.school.studentmanagement.global.enums.AttendanceStatus;
import com.school.studentmanagement.student.dto.StudentMyAttendanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentAttendanceService {
    // 학생의 달 별 출결 현황 맟 휴일(학사 일정)을 반환

    private final AttendanceRepository attendanceRepository;
    private final AcademicCalendarRepository academicCalendarRepository;

    public StudentMyAttendanceResponse getMyMonthlyAttendance(Long studentId, int year, int month) {
        // 시작 날짜, 끝 날짜를 계산
        YearMonth ym = YearMonth.of(year, month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        // 한달 치 출결 데이터를 가져옴
        List<Attendance> records = attendanceRepository
                .findByStudentIdAndDateBetween(studentId, from, to);

        // AttendanceStatus 별로 묶어 개수를 샌다
        Map<AttendanceStatus, Long> counts = records.stream()
                .collect(Collectors.groupingBy(Attendance::getStatus, Collectors.counting()));

        // 출결 데이터(Entity)를 DTO로 변환
        List<StudentMyAttendanceResponse.AttendanceEntry> entries = records.stream()
                .map(a -> StudentMyAttendanceResponse.AttendanceEntry.builder()
                        .date(a.getDate())
                        .status(a.getStatus())
                        .reason(a.getReason())
                        .build())
                .toList();

        // 캘린더 DB에서 해당 연/월에 해당하는 휴일 Entity 리스트를 가져옴
        List<AcademicCalendar> holidays = academicCalendarRepository
                .findHolidaysByYearAndMonth(year, month);

        // Entity -> DTO로 변환
        List<StudentMyAttendanceResponse.HolidayDto> holidayDtos = holidays.stream()
                .map(h -> StudentMyAttendanceResponse.HolidayDto.builder()
                        .date(h.getDate())
                        .name(h.getDescription())
                        .build())
                .toList();

        // 결과 반환 및 null 에러 예방
        return StudentMyAttendanceResponse.builder()
                .year(year)
                .month(month)
                .absentDays(counts.getOrDefault(AttendanceStatus.ABSENT, 0L).intValue())
                .lateDays(counts.getOrDefault(AttendanceStatus.LATE, 0L).intValue())
                .earlyLeaveDays(counts.getOrDefault(AttendanceStatus.EARLY_LEAVE, 0L).intValue())
                .entries(entries)
                .holidays(holidayDtos)
                .build();
    }
}
