package com.school.studentmanagement.grade.service;

import com.school.studentmanagement.analytics.repository.AnalyticsGradeQueryRepository;
import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.global.enums.Gender;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.enums.UserStatus;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.grade.dto.RadarChartResponse;
import com.school.studentmanagement.grade.entity.StudentSemesterStat;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.subject.repository.SubjectAssignmentRepository;
import com.school.studentmanagement.subject.repository.SubjectRepository;
import com.school.studentmanagement.teacher.entity.Teacher;
import com.school.studentmanagement.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GradeAnalyticsServiceTest {

    @InjectMocks private GradeAnalyticsService service;
    @Mock private com.school.studentmanagement.grade.repository.StudentGradeRepository studentGradeRepository;
    @Mock private com.school.studentmanagement.grade.repository.StudentSemesterStatRepository semesterStatRepository;
    @Mock private StudentAffiliationRepository studentAffiliationRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private SubjectAssignmentRepository subjectAssignmentRepository;
    @Mock private com.school.studentmanagement.parent.validator.ParentChildLinkValidator parentChildLinkValidator;
    @Mock private com.school.studentmanagement.student.repository.StudentRepository studentRepository;
    @Mock private com.school.studentmanagement.global.util.AcademicCalendarUtil academicCalendarUtil;
    @Mock private AnalyticsGradeQueryRepository analyticsGradeQueryRepository;

    private static final long STUDENT_ID = 10L;
    private static final long TEACHER_ID = 1L;
    private static final long CLASSROOM_ID = 200L;

    @Nested
    @DisplayName("computeRank — 학기 평균 기준 RANK 산출 (동점 동순위, 다음은 건너뜀)")
    class ComputeRank {

        @Test
        @DisplayName("정상: 평균 내림차순 정렬 후 1, 2, 3등 부여")
        void distinctScores() {
            List<StudentSemesterStat> stats = List.of(
                    statFor(1L, 80.0),
                    statFor(2L, 95.0),
                    statFor(3L, 70.0)
            );

            assertThat(GradeAnalyticsService.computeRank(stats, 2L)).isEqualTo(1);
            assertThat(GradeAnalyticsService.computeRank(stats, 1L)).isEqualTo(2);
            assertThat(GradeAnalyticsService.computeRank(stats, 3L)).isEqualTo(3);
        }

        @Test
        @DisplayName("동점자: 같은 평균이면 같은 등수, 다음은 건너뜀 (1, 1, 3, ...)")
        void tiedScores() {
            List<StudentSemesterStat> stats = List.of(
                    statFor(1L, 90.0),
                    statFor(2L, 90.0),
                    statFor(3L, 80.0),
                    statFor(4L, 70.0)
            );

            assertThat(GradeAnalyticsService.computeRank(stats, 1L)).isEqualTo(1);
            assertThat(GradeAnalyticsService.computeRank(stats, 2L)).isEqualTo(1);
            assertThat(GradeAnalyticsService.computeRank(stats, 3L)).isEqualTo(3);
            assertThat(GradeAnalyticsService.computeRank(stats, 4L)).isEqualTo(4);
        }

        @Test
        @DisplayName("학생 ID가 통계 목록에 없으면 null 반환")
        void studentNotInStats() {
            List<StudentSemesterStat> stats = List.of(statFor(1L, 80.0));
            assertThat(GradeAnalyticsService.computeRank(stats, 999L)).isNull();
        }

        @Test
        @DisplayName("빈 통계 목록은 null 반환")
        void emptyStats() {
            assertThat(GradeAnalyticsService.computeRank(List.of(), 1L)).isNull();
        }

        private StudentSemesterStat statFor(Long studentId, Double averageScore) {
            User user = User.builder().name("학생" + studentId).build();
            ReflectionTestUtils.setField(user, "id", studentId);
            Student student = Student.builder().user(user).enrollmentYear(2026).build();
            ReflectionTestUtils.setField(student, "id", studentId);
            return StudentSemesterStat.builder()
                    .student(student).academicYear(2026).semester(1)
                    .totalScore(averageScore * 3).averageScore(averageScore)
                    .gradeLevel(com.school.studentmanagement.global.enums.GradeLevel.from(averageScore))
                    .build();
        }
    }

    @Nested
    @DisplayName("교사 레이더 조회 권한 (getRadarForTeacher → validateTeacherCanViewStudent)")
    class TeacherRadarAuth {

        @Test
        @DisplayName("학생 미배정 → STUDENT_NOT_ASSIGNED")
        void notAssigned() {
            given(studentAffiliationRepository.findWithAllDetails(STUDENT_ID, 2026, 1))
                    .willReturn(Optional.empty());
            assertThatThrownBy(() -> service.getRadarForTeacher(TEACHER_ID, STUDENT_ID, 2026, 1))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("담임도 과목담당도 아니면 ACCESS_DENIED")
        void noRelation_denied() {
            StudentAffiliation aff = affiliation(teacher(999L)); // 담임은 다른 교사
            given(studentAffiliationRepository.findWithAllDetails(STUDENT_ID, 2026, 1))
                    .willReturn(Optional.of(aff));
            given(subjectAssignmentRepository.existsByTeacherIdAndClassroomIdAndAcademicYearAndSemester(
                    TEACHER_ID, CLASSROOM_ID, 2026, 1)).willReturn(false);
            assertThatThrownBy(() -> service.getRadarForTeacher(TEACHER_ID, STUDENT_ID, 2026, 1))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("권한이 없습니다");
        }

        @Test
        @DisplayName("담임이면 통과해 레이더 조립(빈 과목)")
        void homeroom_ok() {
            StudentAffiliation aff = affiliation(teacher(TEACHER_ID)); // 담임 == 요청 교사
            given(studentAffiliationRepository.findWithAllDetails(STUDENT_ID, 2026, 1))
                    .willReturn(Optional.of(aff));
            given(analyticsGradeQueryRepository.findStudentSubjectScores(STUDENT_ID, 2026, 1)).willReturn(Map.of());
            given(studentAffiliationRepository.findAllByClassroomId(CLASSROOM_ID)).willReturn(List.of(aff));
            given(analyticsGradeQueryRepository.findClassSubjectAverages(any(), eq(2026), eq(1))).willReturn(Map.of());
            given(subjectRepository.findAllById(any())).willReturn(List.of());

            RadarChartResponse res = service.getRadarForTeacher(TEACHER_ID, STUDENT_ID, 2026, 1);

            assertThat(res.getStudentId()).isEqualTo(STUDENT_ID);
            assertThat(res.getAcademicYear()).isEqualTo(2026);
            assertThat(res.getSubjects()).isEmpty();
        }
    }

    @Test
    @DisplayName("학년도/학기 미지정 시 현재 학기로 보정")
    void radar_defaultsToCurrentSemester() {
        given(academicCalendarUtil.getCurrentAcademicYear()).willReturn(2026);
        given(academicCalendarUtil.getCurrentSemester()).willReturn(2);
        StudentAffiliation aff = affiliation(teacher(TEACHER_ID));
        given(studentAffiliationRepository.findWithAllDetails(STUDENT_ID, 2026, 2)).willReturn(Optional.of(aff));
        given(analyticsGradeQueryRepository.findStudentSubjectScores(STUDENT_ID, 2026, 2)).willReturn(Map.of());
        given(studentAffiliationRepository.findAllByClassroomId(CLASSROOM_ID)).willReturn(List.of(aff));
        given(analyticsGradeQueryRepository.findClassSubjectAverages(any(), anyInt(), anyInt())).willReturn(Map.of());
        given(subjectRepository.findAllById(any())).willReturn(List.of());

        RadarChartResponse res = service.getStudentRadar(STUDENT_ID, null, null);

        assertThat(res.getSemester()).isEqualTo(2);
    }

    @Nested
    @DisplayName("교사 종합뷰 / 추이 / 학부모 위임")
    class OverviewTrendParent {

        @Test
        @DisplayName("교사 종합뷰: 담임·데이터 없음 → 조립(석차 null, 학급규모 1)")
        void overview_homeroom_empty_ok() {
            StudentAffiliation aff = affiliation(teacher(TEACHER_ID));
            given(studentAffiliationRepository.findWithAllDetails(STUDENT_ID, 2026, 1)).willReturn(Optional.of(aff));
            given(semesterStatRepository.findByStudentIdAndAcademicYearAndSemester(STUDENT_ID, 2026, 1))
                    .willReturn(Optional.empty());
            given(studentGradeRepository.aggregateSubjectScoresByStudentAndSemester(STUDENT_ID, 2026, 1))
                    .willReturn(List.of());
            given(studentAffiliationRepository.findAllByClassroomId(CLASSROOM_ID)).willReturn(List.of(aff));
            given(studentGradeRepository.aggregateSubjectScoresByStudentIdsAndSemester(List.of(STUDENT_ID), 2026, 1))
                    .willReturn(List.of());
            given(subjectRepository.findAllById(any())).willReturn(List.of());
            given(studentGradeRepository.findByStudentIdAndAcademicYearAndSemester(STUDENT_ID, 2026, 1))
                    .willReturn(List.of());
            given(semesterStatRepository.findByStudentIdsAndYearAndSemester(List.of(STUDENT_ID), 2026, 1))
                    .willReturn(List.of());

            var res = service.getStudentOverviewForTeacher(TEACHER_ID, STUDENT_ID, 2026, 1);

            assertThat(res.getClassSize()).isEqualTo(1);
            assertThat(res.getClassRank()).isNull();
            assertThat(res.getTotalScore()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("추이: from 미지정 시 입학연도/1학기로 보정, 빈 추이 반환")
        void trend_defaults_empty() {
            given(studentRepository.findById(STUDENT_ID)).willReturn(Optional.of(student(STUDENT_ID)));
            given(academicCalendarUtil.getCurrentAcademicYear()).willReturn(2026);
            given(academicCalendarUtil.getCurrentSemester()).willReturn(1);
            given(analyticsGradeQueryRepository.findStudentSubjectTrend(STUDENT_ID, 20261, 20261)).willReturn(List.of());
            given(subjectRepository.findAllById(any())).willReturn(List.of());

            var res = service.getStudentTrend(STUDENT_ID, null, null, null, null);

            assertThat(res.getFromYear()).isEqualTo(2026);
            assertThat(res.getFromSemester()).isEqualTo(1);
            assertThat(res.getSubjects()).isEmpty();
        }

        @Test
        @DisplayName("학부모 추이: 연결 검증 후 위임")
        void childTrend_validatesAndDelegates() {
            given(studentRepository.findById(STUDENT_ID)).willReturn(Optional.of(student(STUDENT_ID)));
            given(academicCalendarUtil.getCurrentAcademicYear()).willReturn(2026);
            given(academicCalendarUtil.getCurrentSemester()).willReturn(1);
            given(analyticsGradeQueryRepository.findStudentSubjectTrend(STUDENT_ID, 20261, 20261)).willReturn(List.of());
            given(subjectRepository.findAllById(any())).willReturn(List.of());

            var res = service.getChildTrend(20L, STUDENT_ID, null, null, null, null);

            assertThat(res.getStudentId()).isEqualTo(STUDENT_ID);
            org.mockito.Mockito.verify(parentChildLinkValidator).validateLinked(20L, STUDENT_ID);
        }

        @Test
        @DisplayName("교사 추이: 노출 데이터 없으면 현재 학기 관계로 권한 검증")
        void teacherTrend_emptyValidatesCurrentSemester() {
            given(studentRepository.findById(STUDENT_ID)).willReturn(Optional.of(student(STUDENT_ID)));
            given(academicCalendarUtil.getCurrentAcademicYear()).willReturn(2026);
            given(academicCalendarUtil.getCurrentSemester()).willReturn(1);
            given(analyticsGradeQueryRepository.findStudentSubjectTrend(STUDENT_ID, 20261, 20261)).willReturn(List.of());
            given(subjectRepository.findAllById(any())).willReturn(List.of());
            given(studentAffiliationRepository.findWithAllDetails(STUDENT_ID, 2026, 1))
                    .willReturn(Optional.of(affiliation(teacher(TEACHER_ID))));

            var res = service.getTrendForTeacher(TEACHER_ID, STUDENT_ID, null, null, null, null);

            assertThat(res.getSubjects()).isEmpty();
        }
    }

    // ─── 헬퍼 ──────────────────────────────────────────────────────────────
    private Student student(long id) {
        User u = User.builder().id(id).name("학생" + id).gender(Gender.MALE)
                .role(UserRole.STUDENT).status(UserStatus.ACTIVE).build();
        return Student.builder().id(id).user(u).enrollmentYear(2026).build();
    }

    private Teacher teacher(long id) {
        User u = User.builder().id(id).name("교사" + id).gender(Gender.MALE)
                .role(UserRole.TEACHER).status(UserStatus.ACTIVE).build();
        Teacher t = Teacher.builder().user(u).build();
        ReflectionTestUtils.setField(t, "id", id);
        return t;
    }

    private StudentAffiliation affiliation(Teacher homeroom) {
        Classroom classroom = Classroom.builder()
                .academicYear(2026).semester(1).grade(1).classNum(4).homeroomTeacher(homeroom).build();
        ReflectionTestUtils.setField(classroom, "id", CLASSROOM_ID);
        return StudentAffiliation.builder().student(student(STUDENT_ID)).classroom(classroom).studentNum(1).build();
    }
}
