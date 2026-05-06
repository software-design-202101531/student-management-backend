package com.school.studentmanagement.global.config;

import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.global.enums.*;
import com.school.studentmanagement.grade.entity.Exam;
import com.school.studentmanagement.grade.entity.StudentGrade;
import com.school.studentmanagement.grade.entity.StudentSemesterStat;
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
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class InitDataConfig implements CommandLineRunner {

    private final EntityManager em;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        // ==========================================
        // 0. 최고 관리자 계정
        // ==========================================
        Long adminCount = em.createQuery("SELECT count(u) FROM User u WHERE u.role = :role", Long.class)
                .setParameter("role", UserRole.ADMIN)
                .getSingleResult();

        if (adminCount == 0) {
            User adminUser = User.builder()
                    .name("최고관리자")
                    .loginId("admin")
                    .password(passwordEncoder.encode("admin1234!"))
                    .gender(Gender.MALE)
                    .role(UserRole.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .build();
            em.persist(adminUser);
            System.out.println("[초기화] 최고 관리자 계정이 생성되었습니다.");
        }

        // ==========================================
        // 1. 기초 과목 (수학, 국어, 영어)
        // ==========================================
        Long subjectCount = em.createQuery("SELECT count(s) FROM Subject s", Long.class).getSingleResult();
        Subject mathSubject;
        Subject korSubject;
        Subject engSubject;

        if (subjectCount == 0) {
            mathSubject = new Subject("수학");
            korSubject  = new Subject("국어");
            engSubject  = new Subject("영어");
            em.persist(mathSubject);
            em.persist(korSubject);
            em.persist(engSubject);
            System.out.println("[초기화] 기초 과목(수학, 국어, 영어) 데이터가 생성되었습니다.");
        } else {
            mathSubject = em.createQuery("SELECT s FROM Subject s WHERE s.name = '수학'", Subject.class).getSingleResult();
            korSubject  = em.createQuery("SELECT s FROM Subject s WHERE s.name = '국어'", Subject.class).getSingleResult();
            engSubject  = em.createQuery("SELECT s FROM Subject s WHERE s.name = '영어'", Subject.class).getSingleResult();
        }

        // ==========================================
        // 2. 교사 계정 (4명)
        // ==========================================
        Long teacherCount = em.createQuery("SELECT count(t) FROM Teacher t", Long.class).getSingleResult();
        Teacher teacher1;
        Teacher teacher2;
        Teacher teacher3;
        Teacher teacher4;

        if (teacherCount == 0) {
            // teacher01 - 김수학 (수학 담당)
            User tUser1 = User.builder()
                    .loginId("teacher01").password(passwordEncoder.encode("test1234!"))
                    .name("김수학").gender(Gender.FEMALE)
                    .role(UserRole.TEACHER).status(UserStatus.ACTIVE)
                    .build();
            em.persist(tUser1);
            teacher1 = Teacher.builder()
                    .user(tUser1).employeeNumber("EMP2026001").subject(mathSubject)
                    .officeLocation("본관 2층 제1교무실").officePhoneNumber("02-123-4567")
                    .employmentStatus(EmploymentStatus.ACTIVE).build();
            em.persist(teacher1);

            // teacher02 - 박국어 (국어 담당)
            User tUser2 = User.builder()
                    .loginId("teacher02").password(passwordEncoder.encode("test1234!"))
                    .name("박국어").gender(Gender.MALE)
                    .role(UserRole.TEACHER).status(UserStatus.ACTIVE)
                    .build();
            em.persist(tUser2);
            teacher2 = Teacher.builder()
                    .user(tUser2).employeeNumber("EMP2026002").subject(korSubject)
                    .officeLocation("본관 2층 제2교무실").officePhoneNumber("02-123-4568")
                    .employmentStatus(EmploymentStatus.ACTIVE).build();
            em.persist(teacher2);

            // teacher03 - 최수학 (수학 담당, 1학년 4반 담임)
            User tUser3 = User.builder()
                    .loginId("teacher03").password(passwordEncoder.encode("test1234!"))
                    .name("최수학").gender(Gender.MALE)
                    .role(UserRole.TEACHER).status(UserStatus.ACTIVE)
                    .build();
            em.persist(tUser3);
            teacher3 = Teacher.builder()
                    .user(tUser3).employeeNumber("EMP2026003").subject(mathSubject)
                    .officeLocation("본관 2층 제3교무실").officePhoneNumber("02-123-4569")
                    .employmentStatus(EmploymentStatus.ACTIVE).build();
            em.persist(teacher3);

            // teacher04 - 이영어 (영어 담당, 1학년 5반 담임)
            User tUser4 = User.builder()
                    .loginId("teacher04").password(passwordEncoder.encode("test1234!"))
                    .name("이영어").gender(Gender.FEMALE)
                    .role(UserRole.TEACHER).status(UserStatus.ACTIVE)
                    .build();
            em.persist(tUser4);
            teacher4 = Teacher.builder()
                    .user(tUser4).employeeNumber("EMP2026004").subject(engSubject)
                    .officeLocation("본관 2층 제4교무실").officePhoneNumber("02-123-4570")
                    .employmentStatus(EmploymentStatus.ACTIVE).build();
            em.persist(teacher4);

            System.out.println("[초기화] 교사 계정(김수학·박국어·최수학·이영어) 4명이 생성되었습니다.");
        } else {
            teacher1 = em.createQuery("SELECT t FROM Teacher t WHERE t.employeeNumber = 'EMP2026001'", Teacher.class).getSingleResult();
            teacher2 = em.createQuery("SELECT t FROM Teacher t WHERE t.employeeNumber = 'EMP2026002'", Teacher.class).getSingleResult();
            teacher3 = em.createQuery("SELECT t FROM Teacher t WHERE t.employeeNumber = 'EMP2026003'", Teacher.class).getSingleResult();
            teacher4 = em.createQuery("SELECT t FROM Teacher t WHERE t.employeeNumber = 'EMP2026004'", Teacher.class).getSingleResult();
        }

        // ==========================================
        // 3. 학급 생성 + 모든 학생/학부모/성적/기록 초기화
        // ==========================================
        Long classCount = em.createQuery("SELECT count(c) FROM Classroom c WHERE c.academicYear = 2026", Long.class).getSingleResult();

        if (classCount > 0) {
            System.out.println("[초기화] 2026학년도 학급 데이터가 이미 존재하여 초기화를 건너뜁니다.");
            return;
        }

        // ── 학급 5개 생성 ──────────────────────────────────────────
        Classroom class1 = Classroom.builder().academicYear(2026).semester(1).grade(1).classNum(1).homeroomTeacher(teacher1).build();
        Classroom class2 = Classroom.builder().academicYear(2026).semester(1).grade(1).classNum(2).homeroomTeacher(teacher2).build();
        Classroom class3 = Classroom.builder().academicYear(2026).semester(1).grade(1).classNum(3).build(); // 담임 미배정(테스트용)
        Classroom class4 = Classroom.builder().academicYear(2026).semester(1).grade(1).classNum(4).homeroomTeacher(teacher3).build();
        Classroom class5 = Classroom.builder().academicYear(2026).semester(1).grade(1).classNum(5).homeroomTeacher(teacher4).build();
        em.persist(class1); em.persist(class2); em.persist(class3); em.persist(class4); em.persist(class5);
        System.out.println("[초기화] 1학년 1~5반 학급이 생성되었습니다.");

        // ── 기존 테스트용 데이터 (3반 홍길동 + 학부모 초대장) ───────────────
        User pendingUser = User.builder()
                .name("홍길동").role(UserRole.STUDENT).gender(Gender.MALE).status(UserStatus.PENDING).build();
        em.persist(pendingUser);
        Student pendingStudent = Student.builder().user(pendingUser).enrollmentYear(2026).build();
        em.persist(pendingStudent);
        em.persist(StudentAffiliation.builder().student(pendingStudent).classroom(class3).studentNum(15).build());
        em.persist(ParentInvitation.builder()
                .student(pendingStudent).phoneNumber("01012345678").relationType(RelationType.FATHER).build());
        System.out.println("[초기화] 1학년 3반 15번 홍길동(PENDING) 및 학부모 초대장이 생성되었습니다.");

        // ── 4반 학생 20명 (PENDING, 성적 테스트용) ─────────────────────────
        List<Student> class4Students = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            User sUser = User.builder()
                    .name("1-4학생" + String.format("%02d", i))
                    .role(UserRole.STUDENT).gender(i % 2 == 0 ? Gender.FEMALE : Gender.MALE)
                    .status(UserStatus.PENDING).build();
            em.persist(sUser);
            Student s = Student.builder().user(sUser).enrollmentYear(2026).build();
            em.persist(s);
            em.persist(StudentAffiliation.builder().student(s).classroom(class4).studentNum(i).build());
            class4Students.add(s);
        }

        // ── 2반 학생 10명 (PENDING) ──────────────────────────────────────
        for (int i = 1; i <= 10; i++) {
            User sUser = User.builder()
                    .name("1-2학생" + String.format("%02d", i))
                    .role(UserRole.STUDENT).gender(i % 2 == 0 ? Gender.FEMALE : Gender.MALE)
                    .status(UserStatus.PENDING).build();
            em.persist(sUser);
            Student s = Student.builder().user(sUser).enrollmentYear(2026).build();
            em.persist(s);
            em.persist(StudentAffiliation.builder().student(s).classroom(class2).studentNum(i).build());
        }
        System.out.println("[초기화] 1학년 2반(10명), 4반(20명) 학생 데이터가 생성되었습니다.");

        // ==========================================
        // 4. 1학년 5반 학생 5명 (ACTIVE - 로그인 가능)
        // ==========================================
        // loginId: student01~05 / password: test1234!
        String[][] studentInfo = {
                {"student01", "박민준", "MALE"},
                {"student02", "김서연", "FEMALE"},
                {"student03", "이도현", "MALE"},
                {"student04", "최지아", "FEMALE"},
                {"student05", "정현우", "MALE"},
        };

        List<Student> class5Students = new ArrayList<>();
        for (int i = 0; i < studentInfo.length; i++) {
            User sUser = User.builder()
                    .loginId(studentInfo[i][0])
                    .password(passwordEncoder.encode("test1234!"))
                    .name(studentInfo[i][1])
                    .gender("MALE".equals(studentInfo[i][2]) ? Gender.MALE : Gender.FEMALE)
                    .role(UserRole.STUDENT).status(UserStatus.ACTIVE)
                    .build();
            em.persist(sUser);
            Student s = Student.builder().user(sUser).enrollmentYear(2026).build();
            em.persist(s);
            em.persist(StudentAffiliation.builder().student(s).classroom(class5).studentNum(i + 1).build());
            class5Students.add(s);
        }
        System.out.println("[초기화] 1학년 5반 학생 5명 (student01~05) 생성 완료.");

        // ==========================================
        // 5. 1학년 5반 학부모 5명 (ACTIVE - 로그인 가능, ParentStudentMapping 연결)
        // ==========================================
        // loginId: parent01~05 / password: test1234!
        String[][] parentInfo = {
                {"parent01", "박철수", "MALE",   "01011112222", "FATHER"},
                {"parent02", "김미영", "FEMALE",  "01022223333", "MOTHER"},
                {"parent03", "이강민", "MALE",   "01033334444", "FATHER"},
                {"parent04", "최혜진", "FEMALE",  "01044445555", "MOTHER"},
                {"parent05", "정재훈", "MALE",   "01055556666", "FATHER"},
        };

        for (int i = 0; i < parentInfo.length; i++) {
            User pUser = User.builder()
                    .loginId(parentInfo[i][0])
                    .password(passwordEncoder.encode("test1234!"))
                    .name(parentInfo[i][1])
                    .gender("MALE".equals(parentInfo[i][2]) ? Gender.MALE : Gender.FEMALE)
                    .role(UserRole.PARENT).status(UserStatus.ACTIVE)
                    .build();
            em.persist(pUser);
            Parent parent = Parent.builder()
                    .user(pUser)
                    .phoneNumber(parentInfo[i][3])
                    .relationType("FATHER".equals(parentInfo[i][4]) ? RelationType.FATHER : RelationType.MOTHER)
                    .build();
            em.persist(parent);
            em.persist(new ParentStudentMapping(parent, class5Students.get(i)));
        }
        System.out.println("[초기화] 1학년 5반 학부모 5명 (parent01~05) 생성 및 자녀 연결 완료.");

        // ==========================================
        // 6. 과목 배정 (SubjectAssignment)
        // ==========================================
        // 기존 반 배정
        em.persist(SubjectAssignment.builder().teacher(teacher3).classroom(class4).subject(mathSubject).academicYear(2026).semester(1).build());
        em.persist(SubjectAssignment.builder().teacher(teacher3).classroom(class2).subject(mathSubject).academicYear(2026).semester(1).build());
        em.persist(SubjectAssignment.builder().teacher(teacher1).classroom(class1).subject(mathSubject).academicYear(2026).semester(1).build());
        em.persist(SubjectAssignment.builder().teacher(teacher2).classroom(class2).subject(korSubject).academicYear(2026).semester(1).build());
        em.persist(SubjectAssignment.builder().teacher(teacher2).classroom(class4).subject(korSubject).academicYear(2026).semester(1).build());
        // 5반 배정 (수학: teacher01, 국어: teacher02, 영어: teacher04)
        em.persist(SubjectAssignment.builder().teacher(teacher1).classroom(class5).subject(mathSubject).academicYear(2026).semester(1).build());
        em.persist(SubjectAssignment.builder().teacher(teacher2).classroom(class5).subject(korSubject).academicYear(2026).semester(1).build());
        em.persist(SubjectAssignment.builder().teacher(teacher4).classroom(class5).subject(engSubject).academicYear(2026).semester(1).build());
        System.out.println("[초기화] 교사별 과목 배정(SubjectAssignment)이 완료되었습니다.");

        // ==========================================
        // 7. 4반 중간고사 성적 (기존)
        // ==========================================
        Exam midterm2026 = Exam.builder().academicYear(2026).semester(1).examType(ExamType.MIDTERM).build();
        em.persist(midterm2026);

        int[] math4Scores = {85, 92, 78, 65, 95, 88, 73, 60, 91, 84, 77, 69, 96, 82, 71, 87, 63, 93, 79, 88};
        int[] kor4Scores  = {90, 85, 72, 80, 88, 75, 82, 91, 68, 86, 94, 77, 83, 71, 89, 76, 95, 70, 84, 78};

        for (int i = 0; i < class4Students.size(); i++) {
            Student s = class4Students.get(i);
            em.persist(StudentGrade.builder().student(s).exam(midterm2026).subject(mathSubject).rawScore(math4Scores[i]).build());
            em.persist(StudentGrade.builder().student(s).exam(midterm2026).subject(korSubject).rawScore(kor4Scores[i]).build());
            int total = math4Scores[i] + kor4Scores[i];
            em.persist(StudentSemesterStat.builder().student(s).academicYear(2026).semester(1).totalScore(total).averageScore(total / 2.0).build());
        }
        System.out.println("[초기화] 1학년 4반 중간고사 성적(수학, 국어) 초기화가 완료되었습니다.");

        // ==========================================
        // 8. 5반 중간고사 성적 (수학·국어·영어)
        // ==========================================
        // [수학, 국어, 영어] 순서
        int[][] mid5Scores = {
                {88, 92, 85},  // student01 박민준
                {95, 80, 91},  // student02 김서연
                {72, 88, 78},  // student03 이도현
                {83, 76, 94},  // student04 최지아
                {91, 84, 87},  // student05 정현우
        };

        for (int i = 0; i < class5Students.size(); i++) {
            Student s = class5Students.get(i);
            em.persist(StudentGrade.builder().student(s).exam(midterm2026).subject(mathSubject).rawScore(mid5Scores[i][0]).build());
            em.persist(StudentGrade.builder().student(s).exam(midterm2026).subject(korSubject).rawScore(mid5Scores[i][1]).build());
            em.persist(StudentGrade.builder().student(s).exam(midterm2026).subject(engSubject).rawScore(mid5Scores[i][2]).build());
        }

        // ==========================================
        // 9. 5반 기말고사 성적 (수학·국어·영어)
        // ==========================================
        Exam final2026 = Exam.builder().academicYear(2026).semester(1).examType(ExamType.FINAL).build();
        em.persist(final2026);

        int[][] fin5Scores = {
                {90, 88, 82},  // student01 박민준
                {97, 85, 89},  // student02 김서연
                {75, 90, 80},  // student03 이도현
                {86, 78, 92},  // student04 최지아
                {93, 87, 85},  // student05 정현우
        };

        for (int i = 0; i < class5Students.size(); i++) {
            Student s = class5Students.get(i);
            em.persist(StudentGrade.builder().student(s).exam(final2026).subject(mathSubject).rawScore(fin5Scores[i][0]).build());
            em.persist(StudentGrade.builder().student(s).exam(final2026).subject(korSubject).rawScore(fin5Scores[i][1]).build());
            em.persist(StudentGrade.builder().student(s).exam(final2026).subject(engSubject).rawScore(fin5Scores[i][2]).build());
        }
        System.out.println("[초기화] 1학년 5반 중간·기말고사 성적(수학·국어·영어) 생성 완료.");

        // ==========================================
        // 10. 5반 학기 통계 (중간 + 기말 합산)
        // ==========================================
        for (int i = 0; i < class5Students.size(); i++) {
            Student s = class5Students.get(i);
            int total = mid5Scores[i][0] + mid5Scores[i][1] + mid5Scores[i][2]
                    + fin5Scores[i][0] + fin5Scores[i][1] + fin5Scores[i][2];
            em.persist(StudentSemesterStat.builder()
                    .student(s).academicYear(2026).semester(1)
                    .totalScore(total).averageScore(total / 6.0)
                    .build());
        }
        System.out.println("[초기화] 1학년 5반 학기 통계(StudentSemesterStat) 생성 완료.");

        // ==========================================
        // 11. 5반 행특 기록 (담임: teacher04)
        // ==========================================
        String[] behaviorContents = {
                "매사에 성실하며 책임감이 강함. 학급 활동에 적극 참여하고 리더십을 발휘함.",
                "호기심이 많고 탐구 정신이 뛰어남. 발표력이 우수하며 논리적으로 의견을 표현함.",
                "차분하고 꼼꼼한 성격으로 맡은 역할을 끝까지 완수함. 협동심이 돋보임.",
                "창의력과 예술적 감수성이 뛰어남. 진로에 대한 목표의식이 뚜렷함.",
                "수리 능력이 우수하고 분석적 사고를 잘 활용함. 친화력이 좋아 급우들과 원만히 지냄.",
        };

        for (int i = 0; i < class5Students.size(); i++) {
            em.persist(StudentRecord.createBehaviorOpinion(
                    class5Students.get(i), teacher4, 2026, 1, behaviorContents[i]));
        }
        System.out.println("[초기화] 1학년 5반 행특 기록 5건 생성 완료.");

        // ==========================================
        // 12. 5반 과세특 기록
        // ==========================================
        // student01 (박민준): 수학·국어·영어
        em.persist(StudentRecord.createSubjectOpinion(class5Students.get(0), teacher1, 2026, 1, mathSubject,
                "수학적 사고력이 뛰어나며 문제 해결 과정을 체계적으로 서술함. 확률·통계 단원에서 높은 이해도를 보임."));
        em.persist(StudentRecord.createSubjectOpinion(class5Students.get(0), teacher2, 2026, 1, korSubject,
                "문학 작품에 대한 감수성이 풍부하며 글쓰기 표현력이 우수함. 토론 수업에서 논거를 명확히 제시함."));
        em.persist(StudentRecord.createSubjectOpinion(class5Students.get(0), teacher4, 2026, 1, engSubject,
                "영어 독해 및 듣기 능력이 탁월함. 영어로 의견을 표현하는 데 자신감 있는 모습을 보임."));

        // student02 (김서연): 수학·영어
        em.persist(StudentRecord.createSubjectOpinion(class5Students.get(1), teacher1, 2026, 1, mathSubject,
                "미적분 개념 이해가 빠르고 응용 문제에 강함. 수업 시간 집중력이 높고 질문이 깊이 있음."));
        em.persist(StudentRecord.createSubjectOpinion(class5Students.get(1), teacher4, 2026, 1, engSubject,
                "영어 회화 능력이 유창하며 원서 독서에 관심이 많음. 영작문 구성력이 뛰어남."));

        // student03 (이도현): 국어
        em.persist(StudentRecord.createSubjectOpinion(class5Students.get(2), teacher2, 2026, 1, korSubject,
                "시 낭독과 문학 분석에서 탁월한 재능을 보임. 독서량이 풍부하여 어휘력과 표현력이 우수함."));

        // student04 (최지아): 영어·수학
        em.persist(StudentRecord.createSubjectOpinion(class5Students.get(3), teacher4, 2026, 1, engSubject,
                "영어 문법 이해도가 높고 에세이 작성에서 논리 구조가 명확함. 외국어에 대한 흥미와 열의가 큼."));
        em.persist(StudentRecord.createSubjectOpinion(class5Students.get(3), teacher1, 2026, 1, mathSubject,
                "기하 단원에서 공간 지각 능력이 두드러짐. 수학적 증명 과정을 정확하게 서술함."));

        // student05 (정현우): 수학
        em.persist(StudentRecord.createSubjectOpinion(class5Students.get(4), teacher1, 2026, 1, mathSubject,
                "수학에 대한 흥미와 집중력이 높음. 복잡한 연립방정식 문제를 다양한 방법으로 풀어내는 능력이 있음."));

        System.out.println("[초기화] 1학년 5반 과세특 기록 9건 생성 완료.");

        // ==========================================
        // 초기화 완료 요약
        // ==========================================
        System.out.println("==========================================================");
        System.out.println("[초기화 완료] 테스트 계정 목록");
        System.out.println("----------------------------------------------------------");
        System.out.println("역할     | loginId    | password  | 비고");
        System.out.println("---------|------------|-----------|---------------------");
        System.out.println("관리자   | admin      | admin1234! |");
        System.out.println("교사(담임)| teacher04  | test1234! | 1-5반 담임, 영어 담당");
        System.out.println("교사(수학)| teacher01  | test1234! | 김수학 (5반 수학)");
        System.out.println("교사(국어)| teacher02  | test1234! | 박국어 (5반 국어)");
        System.out.println("교사(담임)| teacher03  | test1234! | 1-4반 담임, 수학 담당");
        System.out.println("학생     | student01  | test1234! | 5반 1번 박민준");
        System.out.println("학생     | student02  | test1234! | 5반 2번 김서연");
        System.out.println("학생     | student03  | test1234! | 5반 3번 이도현");
        System.out.println("학생     | student04  | test1234! | 5반 4번 최지아");
        System.out.println("학생     | student05  | test1234! | 5반 5번 정현우");
        System.out.println("학부모   | parent01   | test1234! | 박민준 부(父)");
        System.out.println("학부모   | parent02   | test1234! | 김서연 모(母)");
        System.out.println("학부모   | parent03   | test1234! | 이도현 부(父)");
        System.out.println("학부모   | parent04   | test1234! | 최지아 모(母)");
        System.out.println("학부모   | parent05   | test1234! | 정현우 부(父)");
        System.out.println("==========================================================");
    }
}
