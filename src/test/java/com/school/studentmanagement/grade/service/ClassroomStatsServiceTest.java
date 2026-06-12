package com.school.studentmanagement.grade.service;

import com.school.studentmanagement.analytics.repository.AnalyticsGradeQueryRepository;
import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.classroom.repository.ClassRoomRepository;
import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.global.enums.ExamType;
import com.school.studentmanagement.global.enums.Gender;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.enums.UserStatus;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.grade.dto.ClassroomRankingResponse;
import com.school.studentmanagement.grade.dto.ClassroomStatsResponse;
import com.school.studentmanagement.grade.dto.GradeWideRankingResponse;
import com.school.studentmanagement.grade.dto.StudentRankingResponse;
import com.school.studentmanagement.grade.entity.Exam;
import com.school.studentmanagement.grade.entity.StudentSemesterStat;
import com.school.studentmanagement.grade.repository.ExamRepository;
import com.school.studentmanagement.grade.repository.StudentSemesterStatRepository;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.subject.entity.Subject;
import com.school.studentmanagement.subject.repository.SubjectAssignmentRepository;
import com.school.studentmanagement.subject.repository.SubjectRepository;
import com.school.studentmanagement.teacher.entity.Teacher;
import com.school.studentmanagement.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ClassroomStatsServiceTest {

    @InjectMocks private ClassroomStatsService service;
    @Mock private com.school.studentmanagement.grade.repository.StudentGradeRepository studentGradeRepository;
    @Mock private StudentSemesterStatRepository semesterStatRepository;
    @Mock private AnalyticsGradeQueryRepository analyticsGradeQueryRepository;
    @Mock private ExamRepository examRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private SubjectAssignmentRepository subjectAssignmentRepository;
    @Mock private StudentAffiliationRepository studentAffiliationRepository;
    @Mock private ClassRoomRepository classRoomRepository;
    @Mock private com.school.studentmanagement.parent.validator.ParentChildLinkValidator parentChildLinkValidator;
    @Mock private com.school.studentmanagement.global.util.AcademicCalendarUtil academicCalendarUtil;

    private static final long STUDENT_ID = 10L;
    private static final long TEACHER_ID = 1L;
    private static final long CLASSROOM_ID = 200L;

    // ── 학생 본인 학급 석차 ──────────────────────────────────────────────
    @Test
    @DisplayName("내 학급석차: 미배정이면 STUDENT_NOT_ASSIGNED")
    void myRanking_notAssigned() {
        given(studentAffiliationRepository.findWithAllDetails(STUDENT_ID, 2026, 1)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.getMyRanking(STUDENT_ID, 2026, 1))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("내 학급석차: 평균 기준 등수·학급규모·본인평균 반환")
    void myRanking_ok() {
        StudentAffiliation aff = affiliation(STUDENT_ID, teacher(TEACHER_ID));
        given(studentAffiliationRepository.findWithAllDetails(STUDENT_ID, 2026, 1)).willReturn(Optional.of(aff));
        given(studentAffiliationRepository.findAllByClassroomId(CLASSROOM_ID))
                .willReturn(List.of(aff, affiliation(11L, teacher(TEACHER_ID))));
        given(semesterStatRepository.findByStudentIdsAndYearAndSemester(List.of(STUDENT_ID, 11L), 2026, 1))
                .willReturn(List.of(stat(11L, 95.0), stat(STUDENT_ID, 88.0)));

        StudentRankingResponse res = service.getMyRanking(STUDENT_ID, 2026, 1);

        assertThat(res.getRank()).isEqualTo(2);
        assertThat(res.getClassSize()).isEqualTo(2);
        assertThat(res.getAverageScore()).isEqualTo(88.0);
    }

    // ── 학급 석차 (담임 전용) ────────────────────────────────────────────
    @Test
    @DisplayName("학급석차: 담임이 아니면 ACCESS_DENIED")
    void classroomRanking_notHomeroom_denied() {
        given(classRoomRepository.findClassroomByHomeroomTeacherIdAndAcademicYearAndSemester(TEACHER_ID, 2026, 1))
                .willReturn(Optional.empty());
        assertThatThrownBy(() -> service.getClassroomRanking(CLASSROOM_ID, TEACHER_ID, 2026, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("담임");
    }

    @Test
    @DisplayName("학급석차: 담임이면 동점 동순위로 석차 산출")
    void classroomRanking_homeroom_ok() {
        Classroom c = classroom(teacher(TEACHER_ID));
        given(classRoomRepository.findClassroomByHomeroomTeacherIdAndAcademicYearAndSemester(TEACHER_ID, 2026, 1))
                .willReturn(Optional.of(c));
        StudentAffiliation a1 = affiliation(STUDENT_ID, teacher(TEACHER_ID));
        StudentAffiliation a2 = affiliation(11L, teacher(TEACHER_ID));
        given(studentAffiliationRepository.findAllByClassroomId(CLASSROOM_ID)).willReturn(List.of(a1, a2));
        given(semesterStatRepository.findByStudentIdsAndYearAndSemester(List.of(STUDENT_ID, 11L), 2026, 1))
                .willReturn(List.of(stat(STUDENT_ID, 90.0), stat(11L, 90.0)));

        ClassroomRankingResponse res = service.getClassroomRanking(CLASSROOM_ID, TEACHER_ID, 2026, 1);

        assertThat(res.getClassSize()).isEqualTo(2);
        assertThat(res.getRankings()).allMatch(e -> e.getRank() == 1); // 동점 → 둘 다 1등
    }

    // ── 학급 통계 ────────────────────────────────────────────────────────
    @Test
    @DisplayName("학급통계: 시험 없으면 EXAM_NOT_FOUND")
    void classroomStats_examNotFound() {
        given(examRepository.findById(5L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.getClassroomStats(CLASSROOM_ID, TEACHER_ID, 5L, 3L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("학급통계: 담임+분석데이터 없음 → 0건·10구간 분포")
    void classroomStats_noDist_ok() {
        Exam exam = new Exam(2026, 1, ExamType.MIDTERM, "중간고사", 100, 0.3, null, null, true);
        ReflectionTestUtils.setField(exam, "id", 5L);
        given(examRepository.findById(5L)).willReturn(Optional.of(exam));
        given(classRoomRepository.findById(CLASSROOM_ID)).willReturn(Optional.of(classroom(teacher(TEACHER_ID))));
        Subject subject = new Subject("수학");
        ReflectionTestUtils.setField(subject, "id", 3L);
        given(subjectRepository.findById(3L)).willReturn(Optional.of(subject));
        given(analyticsGradeQueryRepository.findClassroomExamSubjectStats(CLASSROOM_ID, 5L, 3L))
                .willReturn(Optional.empty());

        ClassroomStatsResponse res = service.getClassroomStats(CLASSROOM_ID, TEACHER_ID, 5L, 3L);

        assertThat(res.getStudentCount()).isZero();
        assertThat(res.getDistribution()).hasSize(10);
        assertThat(res.getSubjectName()).isEqualTo("수학");
    }

    // ── 학년 단위 석차 (교사) ────────────────────────────────────────────
    @Test
    @DisplayName("학년석차: 동점 동순위 + 다음 등수 건너뜀")
    void gradeWideRanking_ties() {
        given(studentAffiliationRepository.findAllByYearAndSemesterAndGrade(2026, 1, 1))
                .willReturn(List.of(affiliation(STUDENT_ID, teacher(TEACHER_ID)),
                        affiliation(11L, teacher(TEACHER_ID)),
                        affiliation(12L, teacher(TEACHER_ID))));
        given(semesterStatRepository.findByStudentIdsAndYearAndSemester(List.of(STUDENT_ID, 11L, 12L), 2026, 1))
                .willReturn(List.of(stat(STUDENT_ID, 90.0), stat(11L, 90.0), stat(12L, 80.0)));

        GradeWideRankingResponse res = service.getGradeWideRanking(2026, 1, 1);

        assertThat(res.getStudentCount()).isEqualTo(3);
        assertThat(res.getRankings()).hasSize(3);
        // 상위 2명 공동 1등, 3번째는 3등
        assertThat(res.getRankings().stream().filter(e -> e.getRank() == 1).count()).isEqualTo(2);
        assertThat(res.getRankings().get(2).getRank()).isEqualTo(3);
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

    private Classroom classroom(Teacher homeroom) {
        Classroom c = Classroom.builder()
                .academicYear(2026).semester(1).grade(1).classNum(4).homeroomTeacher(homeroom).build();
        ReflectionTestUtils.setField(c, "id", CLASSROOM_ID);
        return c;
    }

    private StudentAffiliation affiliation(long studentId, Teacher homeroom) {
        return StudentAffiliation.builder()
                .student(student(studentId)).classroom(classroom(homeroom)).studentNum((int) studentId).build();
    }

    private StudentSemesterStat stat(long studentId, double avg) {
        return StudentSemesterStat.builder()
                .student(student(studentId)).academicYear(2026).semester(1)
                .totalScore(avg * 3).averageScore(avg)
                .gradeLevel(com.school.studentmanagement.global.enums.GradeLevel.from(avg)).build();
    }
}
