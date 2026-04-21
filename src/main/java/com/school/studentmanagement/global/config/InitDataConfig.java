package com.school.studentmanagement.global.config;

import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.global.enums.EmploymentStatus;
import com.school.studentmanagement.global.enums.ExamType;
import com.school.studentmanagement.global.enums.Gender;
import com.school.studentmanagement.grade.entity.Exam;
import com.school.studentmanagement.grade.entity.StudentGrade;
import com.school.studentmanagement.grade.entity.StudentSemesterStat;
import com.school.studentmanagement.subject.entity.Subject;
import com.school.studentmanagement.subject.entity.SubjectAssignment;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.teacher.entity.Teacher;
import com.school.studentmanagement.user.entity.User;
import com.school.studentmanagement.parent.entity.ParentInvitation;
import com.school.studentmanagement.global.enums.RelationType;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.enums.UserStatus;
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
        // 0. 최고 관리자 계정 생성
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
        // 1. 기초 메타 데이터 (과목) 생성
        // ==========================================
        Long subjectCount = em.createQuery("SELECT count(s) FROM Subject s", Long.class).getSingleResult();
        Subject mathSubject = null;
        Subject korSubject = null;

        if (subjectCount == 0) {
            mathSubject = new Subject("수학");
            korSubject = new Subject("국어");
            em.persist(mathSubject);
            em.persist(korSubject);
            System.out.println("[초기화] 기초 과목(수학, 국어) 데이터가 생성되었습니다.");
        } else {
            mathSubject = em.createQuery("SELECT s FROM Subject s WHERE s.name = '수학'", Subject.class).getSingleResult();
            korSubject = em.createQuery("SELECT s FROM Subject s WHERE s.name = '국어'", Subject.class).getSingleResult();
        }

        // ==========================================
        // 2. 교사 계정 생성 (담임 배정용)
        // ==========================================
        Long teacherCount = em.createQuery("SELECT count(t) FROM Teacher t", Long.class).getSingleResult();
        Teacher teacher1 = null;
        Teacher teacher2 = null;
        Teacher teacher3 = null;

        if (teacherCount == 0) {
            // 교사 1 - User 생성
            User tUser1 = User.builder()
                    .loginId("teacher01")
                    .password(passwordEncoder.encode("test1234!"))
                    .name("김수학")
                    .gender(Gender.FEMALE)
                    .role(UserRole.TEACHER)
                    .status(UserStatus.ACTIVE)
                    .build();
            em.persist(tUser1);

            // 교사 1 - Teacher 상세 정보 생성
            teacher1 = Teacher.builder()
                    .user(tUser1)
                    .employeeNumber("EMP2026001")
                    .subject(mathSubject)
                    .officeLocation("본관 2층 제1교무실")
                    .officePhoneNumber("02-123-4567")
                    .employmentStatus(EmploymentStatus.ACTIVE)
                    .build();
            em.persist(teacher1);

            // 교사 2 - User 생성
            User tUser2 = User.builder()
                    .loginId("teacher02")
                    .password(passwordEncoder.encode("test1234!"))
                    .name("박국어")
                    .gender(Gender.MALE)
                    .role(UserRole.TEACHER)
                    .status(UserStatus.ACTIVE)
                    .build();
            em.persist(tUser2);

            // 교사 2 - Teacher 상세 정보 생성
            teacher2 = Teacher.builder()
                    .user(tUser2)
                    .employeeNumber("EMP2026002")
                    .subject(korSubject)
                    .officeLocation("본관 2층 제2교무실")
                    .officePhoneNumber("02-123-4568")
                    .employmentStatus(EmploymentStatus.ACTIVE)
                    .build();
            em.persist(teacher2);

            // 교사 3 - User 생성 (1학년 4반 담임 겸 수학 담당)
            User tUser3 = User.builder()
                    .loginId("teacher3")
                    .password(passwordEncoder.encode("test1234"))
                    .name("최수학")
                    .gender(Gender.MALE)
                    .role(UserRole.TEACHER)
                    .status(UserStatus.ACTIVE)
                    .build();
            em.persist(tUser3);

            // 교사 3 - Teacher 상세 정보 생성
            teacher3 = Teacher.builder()
                    .user(tUser3)
                    .employeeNumber("EMP2026003")
                    .subject(mathSubject)
                    .officeLocation("본관 2층 제3교무실")
                    .officePhoneNumber("02-123-4569")
                    .employmentStatus(EmploymentStatus.ACTIVE)
                    .build();
            em.persist(teacher3);

            System.out.println("[초기화] 교사 계정(김수학, 박국어, 최수학)이 생성되었습니다.");
        } else {
            teacher1 = em.createQuery("SELECT t FROM Teacher t WHERE t.employeeNumber = 'EMP2026001'", Teacher.class).getSingleResult();
            teacher2 = em.createQuery("SELECT t FROM Teacher t WHERE t.employeeNumber = 'EMP2026002'", Teacher.class).getSingleResult();
            teacher3 = em.createQuery("SELECT t FROM Teacher t WHERE t.employeeNumber = 'EMP2026003'", Teacher.class).getSingleResult();
        }


        // ==========================================
        // 3. 학급 생성 및 담임 배정
        // ==========================================
        Long classCount = em.createQuery("SELECT count(c) FROM Classroom c WHERE c.academicYear = 2026", Long.class).getSingleResult();

        if (classCount == 0) {
            // 1학년 1반 (담임: 김수학)
            Classroom class1 = Classroom.builder()
                    .academicYear(2026)
                    .semester(1)
                    .grade(1)
                    .classNum(1)
                    .homeroomTeacher(teacher1)
                    .build();
            em.persist(class1);

            // 1학년 2반 (담임: 박국어)
            Classroom class2 = Classroom.builder()
                    .academicYear(2026)
                    .semester(1)
                    .grade(1)
                    .classNum(2)
                    .homeroomTeacher(teacher2)
                    .build();
            em.persist(class2);

            // 1학년 3반 (전학생 테스트용 - 담임 미배정)
            Classroom class3 = Classroom.builder()
                    .academicYear(2026)
                    .semester(1)
                    .grade(1)
                    .classNum(3)
                    .build();
            em.persist(class3);

            // 1학년 4반 (담임: 최수학)
            Classroom class4 = Classroom.builder()
                    .academicYear(2026)
                    .semester(1)
                    .grade(1)
                    .classNum(4)
                    .homeroomTeacher(teacher3)
                    .build();
            em.persist(class4);

            System.out.println("[초기화] 학급 생성 및 담임 교사 배정이 완료되었습니다.");

            // ==========================================
            // 4. 테스트용 학생 및 학급 소속/초대장 생성 (1학년 3반)
            // ==========================================
            User user = User.builder()
                    .name("홍길동")
                    .role(UserRole.STUDENT)
                    .gender(Gender.MALE)
                    .status(UserStatus.PENDING)
                    .build();
            em.persist(user);

            Student student = Student.builder()
                    .user(user)
                    .enrollmentYear(2026)
                    .build();
            em.persist(student);

            StudentAffiliation affiliation = StudentAffiliation.builder()
                    .student(student)
                    .classroom(class3)
                    .studentNum(15)
                    .build();
            em.persist(affiliation);

            ParentInvitation invitation = ParentInvitation.builder()
                    .student(student)
                    .phoneNumber("010-1234-5678".replaceAll("-", ""))
                    .relationType(RelationType.FATHER)
                    .build();
            em.persist(invitation);

            System.out.println("[초기화] 1학년 3반 15번 홍길동 학생 및 초대장 데이터가 생성되었습니다.");

            // ==========================================
            // 5. 1학년 4반 학생 20명 생성
            // ==========================================
            List<Student> class4Students = new ArrayList<>();
            for (int i = 1; i <= 20; i++) {
                User sUser = User.builder()
                        .name("1-4학생" + String.format("%02d", i))
                        .role(UserRole.STUDENT)
                        .gender(i % 2 == 0 ? Gender.FEMALE : Gender.MALE)
                        .status(UserStatus.PENDING)
                        .build();
                em.persist(sUser);

                Student sStudent = Student.builder()
                        .user(sUser)
                        .enrollmentYear(2026)
                        .build();
                em.persist(sStudent);

                StudentAffiliation sAffiliation = StudentAffiliation.builder()
                        .student(sStudent)
                        .classroom(class4)
                        .studentNum(i)
                        .build();
                em.persist(sAffiliation);
                class4Students.add(sStudent);
            }
            System.out.println("[초기화] 1학년 4반 학생 20명 데이터가 생성되었습니다.");

            // ==========================================
            // 6. 1학년 2반 학생 10명 생성
            // ==========================================
            for (int i = 1; i <= 10; i++) {
                User sUser = User.builder()
                        .name("1-2학생" + String.format("%02d", i))
                        .role(UserRole.STUDENT)
                        .gender(i % 2 == 0 ? Gender.FEMALE : Gender.MALE)
                        .status(UserStatus.PENDING)
                        .build();
                em.persist(sUser);

                Student sStudent = Student.builder()
                        .user(sUser)
                        .enrollmentYear(2026)
                        .build();
                em.persist(sStudent);

                StudentAffiliation sAffiliation = StudentAffiliation.builder()
                        .student(sStudent)
                        .classroom(class2)
                        .studentNum(i)
                        .build();
                em.persist(sAffiliation);
            }
            System.out.println("[초기화] 1학년 2반 학생 10명 데이터가 생성되었습니다.");

            // ==========================================
            // 7. 과목 배정(SubjectAssignment) 생성
            // ==========================================
            // 최수학(teacher3) → 1학년 4반(class4) 수학 담당
            SubjectAssignment assignment1 = SubjectAssignment.builder()
                    .teacher(teacher3)
                    .classroom(class4)
                    .subject(mathSubject)
                    .academicYear(2026)
                    .semester(1)
                    .build();
            em.persist(assignment1);

            // 최수학(teacher3) → 1학년 2반(class2) 수학 담당
            SubjectAssignment assignment2 = SubjectAssignment.builder()
                    .teacher(teacher3)
                    .classroom(class2)
                    .subject(mathSubject)
                    .academicYear(2026)
                    .semester(1)
                    .build();
            em.persist(assignment2);

            // 김수학(teacher1) → 1학년 1반(class1) 수학 담당
            SubjectAssignment assignment3 = SubjectAssignment.builder()
                    .teacher(teacher1)
                    .classroom(class1)
                    .subject(mathSubject)
                    .academicYear(2026)
                    .semester(1)
                    .build();
            em.persist(assignment3);

            // 박국어(teacher2) → 1학년 2반(class2) 국어 담당
            SubjectAssignment assignment4 = SubjectAssignment.builder()
                    .teacher(teacher2)
                    .classroom(class2)
                    .subject(korSubject)
                    .academicYear(2026)
                    .semester(1)
                    .build();
            em.persist(assignment4);

            // 박국어(teacher2) → 1학년 4반(class4) 국어 담당
            SubjectAssignment assignment5 = SubjectAssignment.builder()
                    .teacher(teacher2)
                    .classroom(class4)
                    .subject(korSubject)
                    .academicYear(2026)
                    .semester(1)
                    .build();
            em.persist(assignment5);

            System.out.println("[초기화] 교사별 과목 배정(SubjectAssignment)이 완료되었습니다.");

            // ==========================================
            // 8. 1학년 4반 중간고사 성적 초기화
            // ==========================================
            Exam midterm2026 = Exam.builder()
                    .academicYear(2026)
                    .semester(1)
                    .examType(ExamType.MIDTERM)
                    .build();
            em.persist(midterm2026);

            int[] mathScores = {85, 92, 78, 65, 95, 88, 73, 60, 91, 84, 77, 69, 96, 82, 71, 87, 63, 93, 79, 88};
            int[] korScores  = {90, 85, 72, 80, 88, 75, 82, 91, 68, 86, 94, 77, 83, 71, 89, 76, 95, 70, 84, 78};

            for (int i = 0; i < class4Students.size(); i++) {
                Student s = class4Students.get(i);

                em.persist(StudentGrade.builder()
                        .student(s)
                        .exam(midterm2026)
                        .subject(mathSubject)
                        .rawScore(mathScores[i])
                        .build());

                em.persist(StudentGrade.builder()
                        .student(s)
                        .exam(midterm2026)
                        .subject(korSubject)
                        .rawScore(korScores[i])
                        .build());

                int total = mathScores[i] + korScores[i];
                em.persist(StudentSemesterStat.builder()
                        .student(s)
                        .academicYear(2026)
                        .semester(1)
                        .totalScore(total)
                        .averageScore(total / 2.0)
                        .build());
            }
            System.out.println("[초기화] 1학년 4반 중간고사 성적(수학, 국어) 초기화가 완료되었습니다.");

        } else {
            System.out.println("[초기화] 2026학년도 학급 데이터가 이미 존재하여 초기화를 건너뜁니다.");
        }
    }
}
