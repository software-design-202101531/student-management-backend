package com.school.studentmanagement.global.config;

import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.consultation.entity.Consultation;
import com.school.studentmanagement.attendance.entity.AcademicCalendar;
import com.school.studentmanagement.attendance.entity.Attendance;
import com.school.studentmanagement.assignment.entity.Assignment;
import com.school.studentmanagement.assignment.entity.Submission;
import com.school.studentmanagement.feedback.entity.Feedback;
import com.school.studentmanagement.global.enums.*;
import com.school.studentmanagement.grade.entity.Exam;
import com.school.studentmanagement.grade.entity.GradeHistory;
import com.school.studentmanagement.grade.entity.GradePolicy;
import com.school.studentmanagement.grade.entity.SemesterClosure;
import com.school.studentmanagement.grade.entity.StudentGrade;
import com.school.studentmanagement.grade.entity.StudentSemesterStat;
import com.school.studentmanagement.notification.entity.Notification;
import com.school.studentmanagement.parent.entity.Parent;
import com.school.studentmanagement.parent.entity.ParentInvitation;
import com.school.studentmanagement.parent.entity.ParentStudentMapping;
import com.school.studentmanagement.record.entity.StudentRecord;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.subject.entity.Subject;
import com.school.studentmanagement.subject.entity.SubjectAssignment;
import com.school.studentmanagement.teacher.entity.Teacher;
import com.school.studentmanagement.user.entity.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 배포 서버 데모용 시드 데이터 생성기.
 *
 * <p>운영 프로필에서도 동작을 시연할 수 있도록 <b>모든 도메인 테이블</b>에 데모 데이터를 넣는다.
 * Flyway(SQL)가 아니라 <b>JPA를 경유</b>해 저장하므로 PII 컬럼 암호화(AES)·비밀번호 BCrypt 해시가 정상 적용된다.</p>
 *
 * <p>구성: 6과목(국어·수학·과학·영어·체육·사회), 6교사(사회 제외 5명이 1~5반 담임이며 각자 과목을 전 반에서 담당,
 * 사회 교사는 5개 반 사회를 담당하되 담임은 아님), 5개 반 × 5명 = 25 학생(ACTIVE), 25 학부모,
 * 중간·기말 시험과 전 과목 성적/학기통계, 출결+학사일정, 피드백, 상담, 과제+제출, 생활기록, 알림,
 * 학기마감/성적변경이력/초대장.</p>
 *
 * <p>멱등: 2026학년도 학급이 이미 있으면 전체 시딩을 건너뛴다. 재시딩하려면 DB 볼륨을 비우고 재기동한다.</p>
 *
 * <p>{@link DemoDataInitializer} 가 {@code app.demo.enabled=true} (prod) 일 때만 호출하며, 시딩 후 분석 ETL을 돌린다.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder {

    private static final int YEAR = 2026;
    private static final int SEMESTER = 1;
    private static final String DEFAULT_PASSWORD = "test1234!";
    private static final int CLASS_COUNT = 5;
    private static final int STUDENTS_PER_CLASS = 5;

    // 과목/교사 인덱스 (0~4 교사는 각각 1~5반 담임, 5번=사회는 담임 없음)
    private static final String[] SUBJECT_NAMES = {"국어", "수학", "과학", "영어", "체육", "사회"};
    private static final String[] TEACHER_NAMES = {"김국어", "이수학", "박과학", "최영어", "정체육", "한사회"};

    private final EntityManager em;
    private final PasswordEncoder passwordEncoder;

    /**
     * @return 이번 호출에서 실제로 시딩했으면 true, 이미 데이터가 있어 건너뛰었으면 false.
     */
    @Transactional
    public boolean seedIfNeeded() {
        Long classCount = em.createQuery(
                        "SELECT count(c) FROM Classroom c WHERE c.academicYear = :y", Long.class)
                .setParameter("y", YEAR)
                .getSingleResult();
        if (classCount > 0) {
            log.info("[demo-seed] {}학년도 학급이 이미 존재 — 데모 시딩을 건너뜁니다.", YEAR);
            return false;
        }

        log.info("[demo-seed] 데모 데이터 시딩 시작");

        ensureAdmin();
        ensureGradePolicy();

        Subject[] subjects = createSubjects();
        Teacher[] teachers = createTeachers(subjects);
        Classroom[] classes = createClassrooms(teachers);
        createSubjectAssignments(teachers, classes, subjects);

        // 학생/학부모
        List<Student> students = createStudentsAndParents(teachers, classes);
        em.flush(); // 이후 알림 등에서 user id 사용을 위해 식별자 확정

        // 온보딩 시연용 PENDING 학생 + 학부모 초대장 (학생 활성화 / 학부모 verify·register 흐름)
        createOnboardingDemo(teachers, classes);

        // 시험·성적·학기통계
        Exam midterm = createExam(ExamType.MIDTERM, "1학기 중간고사", LocalDate.of(YEAR, 4, 24), "3~4월 전 범위");
        Exam finalExam = createExam(ExamType.FINAL, "1학기 기말고사", LocalDate.of(YEAR, 6, 26), "5~6월 전 범위");
        int[][] midScores = new int[students.size()][SUBJECT_NAMES.length];
        int[][] finScores = new int[students.size()][SUBJECT_NAMES.length];
        createGrades(students, subjects, midterm, finalExam, midScores, finScores);
        createSemesterStats(students, midScores, finScores);
        createGradeHistories(students, subjects, midterm);

        // 학사일정 + 출결
        List<LocalDate> schoolDays = createAcademicCalendar();
        createAttendances(students, teachers, classes, schoolDays);

        // 피드백 / 상담 / 과제+제출 / 생활기록 / 알림
        createFeedbacks(students, teachers, classes, midterm);
        createConsultations(students, teachers, classes);
        createAssignmentsAndSubmissions(students, teachers, classes, subjects);
        createRecords(students, teachers, classes, subjects);
        createMiscNotifications(students, midterm);

        // 학기 마감(과거 학기 1건 — 현재 학기 편집을 막지 않도록 2025-2)
        em.persist(SemesterClosure.builder()
                .academicYear(2025).semester(2).method(SemesterClosureMethod.MANUAL)
                .closedByName("최고관리자").reason("데모용 과거 학기 마감 기록").filledCount(0)
                .build());

        log.info("[demo-seed] 데모 데이터 시딩 완료 — 학생 {}명, 반 {}개, 과목 {}개", students.size(), CLASS_COUNT, SUBJECT_NAMES.length);
        return true;
    }

    // ─────────────────────────────────────────────────────────────

    private void ensureAdmin() {
        Long adminCount = em.createQuery("SELECT count(u) FROM User u WHERE u.role = :r", Long.class)
                .setParameter("r", UserRole.ADMIN).getSingleResult();
        if (adminCount == 0) {
            em.persist(User.createActive("admin", passwordEncoder.encode("admin1234!"),
                    "최고관리자", Gender.MALE, UserRole.ADMIN));
            log.info("[demo-seed] 데모 관리자(admin/admin1234!) 생성");
        }
    }

    private void ensureGradePolicy() {
        Long policyCount = em.createQuery("SELECT count(p) FROM GradePolicy p", Long.class).getSingleResult();
        if (policyCount == 0) {
            em.persist(GradePolicy.builder()
                    .name("5단계 성취평가 (기본)").active(true)
                    .aMinScore(90.0).bMinScore(80.0).cMinScore(70.0).dMinScore(60.0)
                    .build());
        }
    }

    private Subject[] createSubjects() {
        Subject[] subjects = new Subject[SUBJECT_NAMES.length];
        for (int i = 0; i < SUBJECT_NAMES.length; i++) {
            subjects[i] = new Subject(SUBJECT_NAMES[i]);
            em.persist(subjects[i]);
        }
        return subjects;
    }

    private Teacher[] createTeachers(Subject[] subjects) {
        Teacher[] teachers = new Teacher[TEACHER_NAMES.length];
        for (int i = 0; i < TEACHER_NAMES.length; i++) {
            User u = User.createActive(
                    String.format("teacher%02d", i + 1), passwordEncoder.encode(DEFAULT_PASSWORD),
                    TEACHER_NAMES[i], i % 2 == 0 ? Gender.MALE : Gender.FEMALE, UserRole.TEACHER);
            em.persist(u);
            teachers[i] = Teacher.builder()
                    .user(u)
                    .employeeNumber(String.format("EMP%d%03d", YEAR, i + 1))
                    .subject(subjects[i])
                    .officeLocation("본관 " + (i + 1) + "층 교무실")
                    .officePhoneNumber(String.format("02-500-%04d", 1000 + i))
                    .employmentStatus(EmploymentStatus.ACTIVE)
                    .build();
            em.persist(teachers[i]);
        }
        return teachers;
    }

    private Classroom[] createClassrooms(Teacher[] teachers) {
        Classroom[] classes = new Classroom[CLASS_COUNT];
        for (int c = 0; c < CLASS_COUNT; c++) {
            // 0~4번 교사(국어·수학·과학·영어·체육)가 각각 1~5반 담임. 사회(5번)는 담임 미배정.
            classes[c] = Classroom.builder()
                    .academicYear(YEAR).semester(SEMESTER).grade(1).classNum(c + 1)
                    .homeroomTeacher(teachers[c])
                    .build();
            em.persist(classes[c]);
        }
        return classes;
    }

    private void createSubjectAssignments(Teacher[] teachers, Classroom[] classes, Subject[] subjects) {
        // 모든 과목을 모든 반에서 해당 과목 교사가 담당 (사회 포함) → 6과목 × 5반 = 30건
        for (int c = 0; c < CLASS_COUNT; c++) {
            for (int s = 0; s < subjects.length; s++) {
                em.persist(SubjectAssignment.builder()
                        .teacher(teachers[s]).classroom(classes[c]).subject(subjects[s])
                        .academicYear(YEAR).semester(SEMESTER)
                        .build());
            }
        }
    }

    private List<Student> createStudentsAndParents(Teacher[] teachers, Classroom[] classes) {
        List<Student> students = new ArrayList<>();
        String[] surnames = {"김", "이", "박", "최", "정", "강", "조", "윤", "장", "임"};
        for (int c = 0; c < CLASS_COUNT; c++) {
            for (int k = 0; k < STUDENTS_PER_CLASS; k++) {
                int g = c * STUDENTS_PER_CLASS + k; // 0~24 전역 인덱스
                String name = surnames[g % surnames.length] + "학생" + String.format("%02d", g + 1);
                User sUser = User.createActive(
                        String.format("student%02d", g + 1), passwordEncoder.encode(DEFAULT_PASSWORD),
                        name, g % 2 == 0 ? Gender.MALE : Gender.FEMALE, UserRole.STUDENT);
                em.persist(sUser);
                Student s = Student.builder()
                        .user(sUser)
                        .homeroomTeacher(teachers[c])
                        .address("서울특별시 데모구 데모로 " + (g + 1) + "길 " + ((g * 7) % 50 + 1))
                        .phoneNumber(String.format("010-1%03d-%04d", g, 2000 + g))
                        .enrollmentYear(YEAR)
                        .build();
                em.persist(s);
                em.persist(StudentAffiliation.builder()
                        .student(s).classroom(classes[c]).studentNum(k + 1).build());
                students.add(s);

                // 학부모 1명 + 자녀 연결
                User pUser = User.createActive(
                        String.format("parent%02d", g + 1), passwordEncoder.encode(DEFAULT_PASSWORD),
                        surnames[g % surnames.length] + "학부모" + String.format("%02d", g + 1),
                        g % 2 == 0 ? Gender.MALE : Gender.FEMALE, UserRole.PARENT);
                em.persist(pUser);
                Parent parent = Parent.builder()
                        .user(pUser)
                        .phoneNumber(String.format("010-2%03d-%04d", g, 3000 + g))
                        .relationType(g % 2 == 0 ? RelationType.FATHER : RelationType.MOTHER)
                        .build();
                em.persist(parent);
                em.persist(new ParentStudentMapping(parent, s));
            }
        }
        return students;
    }

    /**
     * 온보딩 흐름 시연 데이터. 1반(classes[0])에 studentNum 6~8로 <b>PENDING 학생 3명</b>을 추가하고
     * 각 학생에 <b>학부모 초대장</b>을 발급한다.
     * <ul>
     *   <li>학생: loginId/비밀번호 없이 PENDING → {@code /api/user/verify-student} → {@code activate-student} 흐름 시연</li>
     *   <li>학부모: 초대장 phoneNumber로 {@code /api/parents/verify} → {@code register} 흐름 시연(deterministic 암호화로 검색 일치)</li>
     * </ul>
     */
    private void createOnboardingDemo(Teacher[] teachers, Classroom[] classes) {
        String[] names = {"신입생가", "신입생나", "신입생다"};
        for (int i = 0; i < names.length; i++) {
            User u = User.createPending(names[i], i % 2 == 0 ? Gender.MALE : Gender.FEMALE, UserRole.STUDENT);
            em.persist(u);
            Student s = Student.builder()
                    .user(u).homeroomTeacher(teachers[0]).enrollmentYear(YEAR).build();
            em.persist(s);
            em.persist(StudentAffiliation.builder()
                    .student(s).classroom(classes[0]).studentNum(STUDENTS_PER_CLASS + 1 + i).build());
            em.persist(ParentInvitation.builder()
                    .student(s)
                    .phoneNumber(String.format("010-9000-%04d", 1000 + i))
                    .relationType(i % 2 == 0 ? RelationType.FATHER : RelationType.MOTHER)
                    .build());
        }
    }

    private Exam createExam(ExamType type, String name, LocalDate date, String coverage) {
        Exam exam = Exam.builder()
                .academicYear(YEAR).semester(SEMESTER).examType(type).name(name)
                .maxScore(100).weight(0.5).examDate(date).coverage(coverage).published(true)
                .build();
        em.persist(exam);
        return exam;
    }

    /** 학생 g, 과목 s, 시험오프셋(중간0/기말1)로 62~98 사이의 결정적 점수를 만든다. */
    private int score(int g, int s, int examOffset) {
        return 62 + ((g * 5 + s * 9 + examOffset * 7) % 37);
    }

    private void createGrades(List<Student> students, Subject[] subjects, Exam midterm, Exam finalExam,
                              int[][] midScores, int[][] finScores) {
        for (int g = 0; g < students.size(); g++) {
            Student stu = students.get(g);
            for (int s = 0; s < subjects.length; s++) {
                midScores[g][s] = score(g, s, 0);
                finScores[g][s] = score(g, s, 1);
                em.persist(StudentGrade.builder()
                        .student(stu).exam(midterm).subject(subjects[s]).rawScore(midScores[g][s]).build());
                em.persist(StudentGrade.builder()
                        .student(stu).exam(finalExam).subject(subjects[s]).rawScore(finScores[g][s]).build());
            }
        }
    }

    private void createSemesterStats(List<Student> students, int[][] midScores, int[][] finScores) {
        int subjectCount = SUBJECT_NAMES.length;
        for (int g = 0; g < students.size(); g++) {
            double total = 0;
            for (int s = 0; s < subjectCount; s++) {
                total += (midScores[g][s] + finScores[g][s]) / 2.0; // 중간0.5 + 기말0.5
            }
            double average = total / subjectCount;
            em.persist(StudentSemesterStat.builder()
                    .student(students.get(g)).academicYear(YEAR).semester(SEMESTER)
                    .totalScore(total).averageScore(average)
                    .gradeLevel(GradeLevel.from(average))
                    .build());
        }
    }

    private void createGradeHistories(List<Student> students, Subject[] subjects, Exam midterm) {
        // 성적 정정 이력 시연 — 앞 3명의 국어 중간 점수가 +3 정정되었다고 가정
        Long adminId = em.createQuery("SELECT u.id FROM User u WHERE u.loginId = 'admin'", Long.class)
                .getResultStream().findFirst().orElse(null);
        for (int g = 0; g < 3; g++) {
            StudentGrade grade = em.createQuery(
                            "SELECT sg FROM StudentGrade sg WHERE sg.student = :stu AND sg.exam = :e AND sg.subject = :sub",
                            StudentGrade.class)
                    .setParameter("stu", students.get(g))
                    .setParameter("e", midterm)
                    .setParameter("sub", subjects[0])
                    .getResultStream().findFirst().orElse(null);
            if (grade == null) continue;
            em.persist(GradeHistory.builder()
                    .studentGrade(grade)
                    .beforeScore(grade.getRawScore() - 3)
                    .afterScore(grade.getRawScore())
                    .changedByUserId(adminId)
                    .changedByName("최고관리자")
                    .reason("채점 오류 정정(데모)")
                    .build());
        }
    }

    private List<LocalDate> createAcademicCalendar() {
        List<LocalDate> schoolDays = new ArrayList<>();
        em.persist(AcademicCalendar.builder()
                .date(LocalDate.of(YEAR, 3, 1)).dayType(DayType.HOLIDAY).description("삼일절").build());
        // 3/2 ~ 3/13 평일을 수업일로
        LocalDate d = LocalDate.of(YEAR, 3, 2);
        LocalDate end = LocalDate.of(YEAR, 3, 13);
        while (!d.isAfter(end)) {
            int dow = d.getDayOfWeek().getValue(); // 6=토,7=일
            if (dow < 6) {
                em.persist(AcademicCalendar.builder()
                        .date(d).dayType(DayType.WEEKDAY).description("정상 수업일").build());
                schoolDays.add(d);
            }
            d = d.plusDays(1);
        }
        em.persist(AcademicCalendar.builder()
                .date(LocalDate.of(YEAR, 3, 16)).dayType(DayType.EVENT).description("개교기념 행사").build());
        return schoolDays;
    }

    private void createAttendances(List<Student> students, Teacher[] teachers, Classroom[] classes,
                                   List<LocalDate> schoolDays) {
        for (int g = 0; g < students.size(); g++) {
            int c = g / STUDENTS_PER_CLASS;
            Teacher homeroom = teachers[c];
            for (int di = 0; di < schoolDays.size(); di++) {
                LocalDate day = schoolDays.get(di);
                int key = g + di;
                AttendanceStatus status;
                String reason;
                if (key % 17 == 0) { status = AttendanceStatus.ABSENT; reason = "개인 사정 결석"; }
                else if (key % 13 == 0) { status = AttendanceStatus.LATE; reason = "교통 지연 지각"; }
                else if (key % 19 == 0) { status = AttendanceStatus.EARLY_LEAVE; reason = "병원 진료 조퇴"; }
                else { status = AttendanceStatus.PRESENT; reason = null; }
                em.persist(Attendance.builder()
                        .student(students.get(g)).teacher(homeroom).date(day).status(status).reason(reason)
                        .build());
            }
        }
    }

    private void createFeedbacks(List<Student> students, Teacher[] teachers, Classroom[] classes, Exam midterm) {
        FeedbackCategory[] cats = FeedbackCategory.values();
        for (int g = 0; g < students.size(); g++) {
            int c = g / STUDENTS_PER_CLASS;
            Teacher homeroom = teachers[c];
            FeedbackCategory cat = cats[g % cats.length];
            Feedback fb = Feedback.create(homeroom, students.get(g), cat,
                    "이번 학기 " + cat.name() + " 영역에서 꾸준한 향상을 보였습니다. 앞으로도 성실한 태도를 기대합니다.", true);
            fb.publish(); // 발행
            em.persist(fb);
            // 발행 알림
            em.persist(Notification.builder()
                    .recipientUserId(students.get(g).getUser().getId())
                    .type(NotificationType.FEEDBACK_PUBLISHED)
                    .title("새 피드백이 발행되었습니다")
                    .content(homeroom.getUser().getName() + " 선생님이 피드백을 작성했습니다.")
                    .build());
        }
        // 임시저장(DRAFT) 피드백 2건 — 발행 전 상태 시연
        for (int g = 0; g < 2; g++) {
            em.persist(Feedback.create(teachers[g], students.get(g), FeedbackCategory.ETC,
                    "작성 중인 임시 피드백입니다.", false));
        }
    }

    private void createConsultations(List<Student> students, Teacher[] teachers, Classroom[] classes) {
        ConsultationVisibility[] vis = ConsultationVisibility.values();
        for (int c = 0; c < CLASS_COUNT; c++) {
            Teacher homeroom = teachers[c];
            for (int k = 0; k < 2; k++) { // 반별 2건
                int g = c * STUDENTS_PER_CLASS + k;
                em.persist(Consultation.create(
                        homeroom, students.get(g),
                        LocalDateTime.of(YEAR, 3, 20 + k, 14, 0),
                        "학업 및 교우관계 전반에 대한 상담을 진행함.",
                        "다음 달 진로 희망 조사 후 추가 상담 예정.",
                        vis[(c + k) % vis.length]));
            }
        }
    }

    private void createAssignmentsAndSubmissions(List<Student> students, Teacher[] teachers,
                                                 Classroom[] classes, Subject[] subjects) {
        for (int c = 0; c < CLASS_COUNT; c++) {
            // 반 담임 교사의 과목으로 과제 1건
            Assignment assignment = Assignment.builder()
                    .classroom(classes[c]).subject(subjects[c]).teacher(teachers[c])
                    .title(SUBJECT_NAMES[c] + " " + (c + 1) + "반 과제: 단원 정리 보고서")
                    .description("배운 내용을 정리하여 제출하세요.")
                    .dueDate(LocalDateTime.of(YEAR, 5, 10, 23, 59))
                    .build();
            em.persist(assignment);

            for (int k = 0; k < STUDENTS_PER_CLASS; k++) {
                int g = c * STUDENTS_PER_CLASS + k;
                Student stu = students.get(g);
                // 과제 부여 알림
                em.persist(Notification.builder()
                        .recipientUserId(stu.getUser().getId())
                        .type(NotificationType.ASSIGNMENT_CREATED)
                        .title("새 과제가 등록되었습니다")
                        .content(assignment.getTitle())
                        .build());

                if (k < 3) {
                    // 앞 3명은 제출(SUBMITTED), 그 중 2명은 채점 완료
                    Submission sub = Submission.builder()
                            .assignment(assignment).student(stu)
                            .content("과제를 제출합니다. (데모)")
                            .submittedAt(LocalDateTime.of(YEAR, 5, 8, 10, 0))
                            .status(SubmissionStatus.SUBMITTED)
                            .build();
                    if (k < 2) {
                        sub.grade(90 - k * 5, "충실히 작성했습니다.");
                    }
                    em.persist(sub);
                }
                // 나머지(k>=3)는 미제출 — Submission 미생성(NOT_SUBMITTED 상태로 간주)
            }
        }
    }

    private void createRecords(List<Student> students, Teacher[] teachers, Classroom[] classes, Subject[] subjects) {
        String[] behaviors = {
                "매사에 성실하며 책임감이 강함. 학급 활동에 적극 참여함.",
                "호기심이 많고 탐구 정신이 뛰어남. 발표력이 우수함.",
                "차분하고 꼼꼼하여 맡은 역할을 끝까지 완수함.",
                "창의력과 예술적 감수성이 뛰어나며 목표의식이 뚜렷함.",
                "분석적 사고가 우수하고 친화력이 좋아 급우들과 원만함.",
        };
        for (int g = 0; g < students.size(); g++) {
            int c = g / STUDENTS_PER_CLASS;
            // 행동특성 — 담임 교사
            em.persist(StudentRecord.createBehaviorOpinion(
                    students.get(g), teachers[c], YEAR, SEMESTER, behaviors[g % behaviors.length]));
            // 교과 세특 — 2개 과목(해당 과목 교사)
            int s1 = g % subjects.length;
            int s2 = (g + 2) % subjects.length;
            em.persist(StudentRecord.createSubjectOpinion(
                    students.get(g), teachers[s1], YEAR, SEMESTER, subjects[s1],
                    SUBJECT_NAMES[s1] + " 교과에서 높은 이해도와 적극적인 참여를 보임."));
            if (s2 != s1) {
                em.persist(StudentRecord.createSubjectOpinion(
                        students.get(g), teachers[s2], YEAR, SEMESTER, subjects[s2],
                        SUBJECT_NAMES[s2] + " 교과에서 꾸준한 노력과 향상을 보임."));
            }
        }
    }

    private void createMiscNotifications(List<Student> students, Exam midterm) {
        // 성적 발행 알림 — 학생 전원
        for (Student s : students) {
            em.persist(Notification.builder()
                    .recipientUserId(s.getUser().getId())
                    .type(NotificationType.GRADE_PUBLISHED)
                    .title("성적이 발행되었습니다")
                    .content(midterm.getName() + " 성적을 확인하세요.")
                    .referenceId(midterm.getId())
                    .build());
        }
    }
}
