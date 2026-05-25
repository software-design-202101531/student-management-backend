package com.school.studentmanagement.student.service;

import com.school.studentmanagement.attendance.entity.AcademicCalendar;
import com.school.studentmanagement.attendance.entity.Attendance;
import com.school.studentmanagement.attendance.repository.AcademicCalendarRepository;
import com.school.studentmanagement.attendance.repository.AttendanceRepository;
import com.school.studentmanagement.global.enums.AttendanceStatus;
import com.school.studentmanagement.global.enums.DayType;
import com.school.studentmanagement.student.dto.StudentMyAttendanceResponse;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StudentAttendanceServiceTest {

    @InjectMocks private StudentAttendanceService studentAttendanceService;

    @Mock private AttendanceRepository attendanceRepository;
    @Mock private AcademicCalendarRepository academicCalendarRepository;

    @Test
    @DisplayName("성공: 결석 2 + 지각 1 + 조퇴 1 → 합계 산출 + entries 모두 노출")
    void getMyMonthlyAttendance_aggregatesAndListsEntries() {
        Long studentId = 1L;
        Student student = buildStudent(studentId);

        given(attendanceRepository.findByStudentIdAndDateBetween(eq(studentId), any(), any()))
                .willReturn(List.of(
                        attendance(student, LocalDate.of(2026, 3, 5), AttendanceStatus.ABSENT, "감기"),
                        attendance(student, LocalDate.of(2026, 3, 12), AttendanceStatus.ABSENT, null),
                        attendance(student, LocalDate.of(2026, 3, 18), AttendanceStatus.LATE, "교통체증"),
                        attendance(student, LocalDate.of(2026, 3, 25), AttendanceStatus.EARLY_LEAVE, "병원")
                ));
        given(academicCalendarRepository.findHolidaysByYearAndMonth(eq(2026), eq(3)))
                .willReturn(List.of(
                        AcademicCalendar.builder()
                                .date(LocalDate.of(2026, 3, 1))
                                .dayType(DayType.HOLIDAY).description("삼일절").build()
                ));

        StudentMyAttendanceResponse response = studentAttendanceService.getMyMonthlyAttendance(studentId, 2026, 3);

        assertThat(response.getYear()).isEqualTo(2026);
        assertThat(response.getMonth()).isEqualTo(3);
        assertThat(response.getAbsentDays()).isEqualTo(2);
        assertThat(response.getLateDays()).isEqualTo(1);
        assertThat(response.getEarlyLeaveDays()).isEqualTo(1);
        assertThat(response.getEntries()).hasSize(4);
        assertThat(response.getHolidays()).hasSize(1);
        assertThat(response.getHolidays().get(0).getName()).isEqualTo("삼일절");
    }

    @Test
    @DisplayName("성공: 특이사항 없는 달은 합계 0 + entries 빈 배열")
    void getMyMonthlyAttendance_emptyMonth() {
        Long studentId = 1L;

        given(attendanceRepository.findByStudentIdAndDateBetween(eq(studentId), any(), any()))
                .willReturn(List.of());
        given(academicCalendarRepository.findHolidaysByYearAndMonth(anyInt(), anyInt()))
                .willReturn(List.of());

        StudentMyAttendanceResponse response = studentAttendanceService.getMyMonthlyAttendance(studentId, 2026, 4);

        assertThat(response.getAbsentDays()).isZero();
        assertThat(response.getLateDays()).isZero();
        assertThat(response.getEarlyLeaveDays()).isZero();
        assertThat(response.getEntries()).isEmpty();
        assertThat(response.getHolidays()).isEmpty();
    }

    private Student buildStudent(Long studentId) {
        User user = User.builder().name("학생" + studentId).build();
        ReflectionTestUtils.setField(user, "id", studentId);
        Student student = Student.builder().user(user).enrollmentYear(2026).build();
        ReflectionTestUtils.setField(student, "id", studentId);
        return student;
    }

    private Attendance attendance(Student student, LocalDate date, AttendanceStatus status, String reason) {
        return Attendance.builder()
                .student(student)
                .date(date)
                .status(status)
                .reason(reason)
                .build();
    }
}
