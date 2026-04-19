package com.school.studentmanagement.attendance.service;

import com.school.studentmanagement.StudentAffiliation.entity.StudentAffiliation;
import com.school.studentmanagement.StudentAffiliation.repository.StudentAffiliationRepository;
import com.school.studentmanagement.attendance.dto.AttendanceSaveRequest;
import com.school.studentmanagement.attendance.dto.AttendanceDailyResponse;
import com.school.studentmanagement.attendance.dto.AttendanceMonthlyResponse;
import com.school.studentmanagement.attendance.entity.Attendance;
import com.school.studentmanagement.attendance.repository.AttendanceRepository;
import com.school.studentmanagement.calendar.entity.AcademicCalendar;
import com.school.studentmanagement.calendar.repository.AcademicCalendarRepository;
import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.classroom.repository.ClassRoomRepository;
import com.school.studentmanagement.global.enums.*;
import com.school.studentmanagement.user.entity.Student;
import com.school.studentmanagement.user.entity.Teacher;
import com.school.studentmanagement.user.entity.User;
import com.school.studentmanagement.user.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @InjectMocks private AttendanceService attendanceService;
    @Mock private ClassRoomRepository classRoomRepository;
    @Mock private AcademicCalendarRepository academicCalendarRepository;
    @Mock private StudentAffiliationRepository studentAffiliationRepository;
    @Mock private AttendanceRepository attendanceRepository;
    @Mock private TeacherRepository teacherRepository;

    private static final Long TEACHER_ID   = 100L;
    private static final Long CLASSROOM_ID = 200L;
    private static final Long STUDENT_ID   = 1L;

    private Teacher teacher;
    private Classroom classroom;
    private Student student;
    private StudentAffiliation affiliation;

    @BeforeEach
    void setUp() {
        User teacherUser = User.builder().id(TEACHER_ID).name("최수학")
                .gender(Gender.MALE).role(UserRole.TEACHER).status(UserStatus.ACTIVE).build();
        teacher = Teacher.builder().user(teacherUser).employeeNumber("EMP001")
                .officeLocation("본관").officePhoneNumber("02-000").employmentStatus(EmploymentStatus.ACTIVE).build();
        ReflectionTestUtils.setField(teacher, "id", TEACHER_ID);

        classroom = Classroom.builder().academicYear(2026).semester(1).grade(1).classNum(4)
                .homeroomTeacher(teacher).build();
        ReflectionTestUtils.setField(classroom, "id", CLASSROOM_ID);

        User studentUser = User.builder().id(STUDENT_ID).name("1-4학생01")
                .gender(Gender.MALE).role(UserRole.STUDENT).status(UserStatus.ACTIVE).build();
        student = Student.builder().id(STUDENT_ID).user(studentUser).enrollmentYear(2026).build();
        affiliation = StudentAffiliation.builder().student(student).classroom(classroom).studentNum(1).build();
    }

    // 담임 검증 통과 stubbing 헬퍼
    private void stubValidHomeroomTeacher() {
        given(classRoomRepository.findById(CLASSROOM_ID)).willReturn(Optional.of(classroom));
    }

    // ==========================================================================
    // getMonthlyAttendance
    // ==========================================================================

    @Nested
    @DisplayName("월간 출결 조회 (getMonthlyAttendance)")
    class GetMonthlyAttendanceTest {

        @Test
        @DisplayName("성공: 해당 월 휴일 목록 반환")
        void getMonthlyAttendance_Success() {
            // Given
            AcademicCalendar holiday = AcademicCalendar.builder()
                    .date(LocalDate.of(2026, 5, 5)).dayType(DayType.HOLIDAY).description("어린이날").build();
            stubValidHomeroomTeacher();
            given(academicCalendarRepository.findHolidaysByYearAndMonth(2026, 5)).willReturn(List.of(holiday));

            // When
            AttendanceMonthlyResponse response = attendanceService.getMonthlyAttendance(CLASSROOM_ID, TEACHER_ID, 2026, 5);

            // Then
            assertThat(response.getYear()).isEqualTo(2026);
            assertThat(response.getMonth()).isEqualTo(5);
            assertThat(response.getHolidays()).hasSize(1);
            assertThat(response.getHolidays().get(0).getName()).isEqualTo("어린이날");
        }

        @Test
        @DisplayName("실패: 담임이 아닌 교사 → AccessDeniedException")
        void getMonthlyAttendance_Fail_NotHomeroomTeacher() {
            // Given - classroom의 담임은 다른 선생님
            User otherUser = User.builder().id(999L).name("다른선생").gender(Gender.MALE)
                    .role(UserRole.TEACHER).status(UserStatus.ACTIVE).build();
            Teacher otherTeacher = Teacher.builder().user(otherUser).employeeNumber("EMP999")
                    .officeLocation("본관").officePhoneNumber("02-999").employmentStatus(EmploymentStatus.ACTIVE).build();
            ReflectionTestUtils.setField(otherTeacher, "id", 999L);
            Classroom otherClassroom = Classroom.builder().academicYear(2026).semester(1).grade(1).classNum(1)
                    .homeroomTeacher(otherTeacher).build();

            given(classRoomRepository.findById(CLASSROOM_ID)).willReturn(Optional.of(otherClassroom));

            // When & Then
            assertThatThrownBy(() -> attendanceService.getMonthlyAttendance(CLASSROOM_ID, TEACHER_ID, 2026, 5))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("해당 반의 담임이 아닙니다");
        }
    }

    // ==========================================================================
    // getDailyAttendance
    // ==========================================================================

    @Nested
    @DisplayName("일간 출결 조회 (getDailyAttendance)")
    class GetDailyAttendanceTest {

        @Test
        @DisplayName("성공: 공휴일인 날은 isHoliday=true, holidayName 포함")
        void getDailyAttendance_Success_HolidayDetected() {
            // Given
            LocalDate childrenDay = LocalDate.of(2026, 5, 5);
            AcademicCalendar holiday = AcademicCalendar.builder()
                    .date(childrenDay).dayType(DayType.HOLIDAY).description("어린이날").build();

            stubValidHomeroomTeacher();
            given(academicCalendarRepository.findByDate(childrenDay)).willReturn(Optional.of(holiday));
            given(studentAffiliationRepository.findAllByClassroomId(CLASSROOM_ID)).willReturn(List.of(affiliation));
            given(attendanceRepository.findByStudentIdsAndDate(anyList(), any())).willReturn(List.of());

            // When
            AttendanceDailyResponse response = attendanceService.getDailyAttendance(CLASSROOM_ID, TEACHER_ID, childrenDay);

            // Then
            assertThat(response.isHoliday()).isTrue();
            assertThat(response.getHolidayName()).isEqualTo("어린이날");
        }

        @Test
        @DisplayName("성공: 결석 학생은 ABSENT 상태와 사유 반환, 출석 학생은 PRESENT 반환")
        void getDailyAttendance_Success_AbsentAndPresentStudents() {
            // Given
            LocalDate today = LocalDate.of(2026, 4, 19);
            Attendance absentRecord = Attendance.builder()
                    .student(student).teacher(teacher).date(today)
                    .status(AttendanceStatus.ABSENT).reason("감기").build();

            stubValidHomeroomTeacher();
            given(academicCalendarRepository.findByDate(today)).willReturn(Optional.empty());
            given(studentAffiliationRepository.findAllByClassroomId(CLASSROOM_ID)).willReturn(List.of(affiliation));
            given(attendanceRepository.findByStudentIdsAndDate(anyList(), any())).willReturn(List.of(absentRecord));

            // When
            AttendanceDailyResponse response = attendanceService.getDailyAttendance(CLASSROOM_ID, TEACHER_ID, today);

            // Then
            assertThat(response.isHoliday()).isFalse();
            assertThat(response.getStudents()).hasSize(1);
            assertThat(response.getStudents().get(0).getStatus()).isEqualTo(AttendanceStatus.ABSENT);
            assertThat(response.getStudents().get(0).getReason()).isEqualTo("감기");
        }
    }

    // ==========================================================================
    // saveDailyAttendance
    // ==========================================================================

    @Nested
    @DisplayName("출결 일괄 저장 (saveDailyAttendance)")
    class SaveDailyAttendanceTest {

        @Test
        @DisplayName("실패: 미래 날짜 출결 입력 → IllegalArgumentException")
        void saveDailyAttendance_Fail_FutureDate() {
            // Given
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            stubValidHomeroomTeacher();

            // When & Then
            assertThatThrownBy(() -> attendanceService.saveDailyAttendance(
                    CLASSROOM_ID, TEACHER_ID, tomorrow, buildRequest(STUDENT_ID, AttendanceStatus.ABSENT, "감기")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("출결은 미리 입력할 수 없습니다");
        }

        @Test
        @DisplayName("Case1 성공: PRESENT로 변경 시 기존 결석 기록 삭제")
        void saveDailyAttendance_Success_Case1_PresentDeletesExistingRecord() {
            // Given
            LocalDate today = LocalDate.now();
            Attendance existingAbsence = Attendance.builder()
                    .student(student).teacher(teacher).date(today)
                    .status(AttendanceStatus.ABSENT).reason("감기").build();

            stubValidHomeroomTeacher();
            given(teacherRepository.getReferenceById(TEACHER_ID)).willReturn(teacher);
            given(attendanceRepository.findByStudentIdsAndDate(anyList(), any())).willReturn(List.of(existingAbsence));

            // When
            attendanceService.saveDailyAttendance(
                    CLASSROOM_ID, TEACHER_ID, today, buildRequest(STUDENT_ID, AttendanceStatus.PRESENT, null));

            // Then
            verify(attendanceRepository).delete(existingAbsence);
            verify(attendanceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Case2 성공: 이미 결석 기록이 있으면 updateStatus 호출")
        void saveDailyAttendance_Success_Case2_UpdatesExistingAbsence() {
            // Given
            LocalDate today = LocalDate.now();
            Attendance existingAbsence = Attendance.builder()
                    .student(student).teacher(teacher).date(today)
                    .status(AttendanceStatus.ABSENT).reason("감기").build();

            stubValidHomeroomTeacher();
            given(teacherRepository.getReferenceById(TEACHER_ID)).willReturn(teacher);
            given(attendanceRepository.findByStudentIdsAndDate(anyList(), any())).willReturn(List.of(existingAbsence));

            // When
            attendanceService.saveDailyAttendance(
                    CLASSROOM_ID, TEACHER_ID, today, buildRequest(STUDENT_ID, AttendanceStatus.LATE, "지각 사유"));

            // Then
            assertThat(existingAbsence.getStatus()).isEqualTo(AttendanceStatus.LATE);
            assertThat(existingAbsence.getReason()).isEqualTo("지각 사유");
            verify(attendanceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Case3 성공: 기존 기록 없는 새 결석 → Attendance 신규 저장")
        void saveDailyAttendance_Success_Case3_CreatesNewAbsence() {
            // Given
            LocalDate today = LocalDate.now();

            stubValidHomeroomTeacher();
            given(teacherRepository.getReferenceById(TEACHER_ID)).willReturn(teacher);
            given(attendanceRepository.findByStudentIdsAndDate(anyList(), any())).willReturn(List.of());
            given(studentAffiliationRepository.findByStudentIdAndClassroomId(STUDENT_ID, CLASSROOM_ID))
                    .willReturn(Optional.of(affiliation));

            // When
            attendanceService.saveDailyAttendance(
                    CLASSROOM_ID, TEACHER_ID, today, buildRequest(STUDENT_ID, AttendanceStatus.ABSENT, "감기"));

            // Then
            verify(attendanceRepository).save(any(Attendance.class));
        }

        @Test
        @DisplayName("Case3 실패: 학급 소속이 아닌 학생 → IllegalArgumentException")
        void saveDailyAttendance_Fail_StudentNotInClassroom() {
            // Given
            LocalDate today = LocalDate.now();

            stubValidHomeroomTeacher();
            given(teacherRepository.getReferenceById(TEACHER_ID)).willReturn(teacher);
            given(attendanceRepository.findByStudentIdsAndDate(anyList(), any())).willReturn(List.of());
            given(studentAffiliationRepository.findByStudentIdAndClassroomId(STUDENT_ID, CLASSROOM_ID))
                    .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> attendanceService.saveDailyAttendance(
                    CLASSROOM_ID, TEACHER_ID, today, buildRequest(STUDENT_ID, AttendanceStatus.ABSENT, "감기")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("해당 반의 학생이 아닙니다");
        }
    }

    // ─── 헬퍼 ────────────────────────────────────────────────────────────────────
    private AttendanceSaveRequest buildRequest(Long studentId, AttendanceStatus status, String reason) {
        AttendanceSaveRequest.StudentAttendanceUpdateDto dto = new AttendanceSaveRequest.StudentAttendanceUpdateDto();
        ReflectionTestUtils.setField(dto, "studentId", studentId);
        ReflectionTestUtils.setField(dto, "status", status);
        ReflectionTestUtils.setField(dto, "reason", reason);
        AttendanceSaveRequest request = new AttendanceSaveRequest();
        ReflectionTestUtils.setField(request, "attendanceData", List.of(dto));
        return request;
    }
}
