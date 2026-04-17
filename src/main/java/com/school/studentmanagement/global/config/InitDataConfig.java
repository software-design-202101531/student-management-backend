// 📍 위치: src/main/java/com/school/studentmanagement/global/config/InitDataConfig.java

package com.school.studentmanagement.global.config;

import com.school.studentmanagement.StudentAffiliation.entity.StudentAffiliation;
import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.global.enums.EmploymentStatus;
import com.school.studentmanagement.global.enums.Gender;
import com.school.studentmanagement.subject.entity.Subject;
import com.school.studentmanagement.subject.entity.SubjectAssignment;
import com.school.studentmanagement.user.entity.Student;
import com.school.studentmanagement.user.entity.Teacher;
import com.school.studentmanagement.user.entity.User;
import com.school.studentmanagement.invitation.entity.ParentInvitation;
import com.school.studentmanagement.global.enums.RelationType;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.enums.UserStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

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
            System.out.println("🍠 [춘식이 알림] 최고 관리자 계정 뚝딱 맹글었슈!");
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
            System.out.println("🍠 [춘식이 알림] 기초 과목(수학, 국어) 데이터 세팅 완료!");
        } else {
            mathSubject = em.createQuery("SELECT s FROM Subject s WHERE s.name = '수학'", Subject.class).getSingleResult();
            korSubject = em.createQuery("SELECT s FROM Subject s WHERE s.name = '국어'", Subject.class).getSingleResult();
        }

        // ==========================================
        // 2. 선생님 계정 생성 (담임 배정용)
        // ==========================================
        Long teacherCount = em.createQuery("SELECT count(t) FROM Teacher t", Long.class).getSingleResult();
        Teacher teacher1 = null;
        Teacher teacher2 = null;
        Teacher teacher3 = null;

        if (teacherCount == 0) {
            // 선생님 1 몸통(User) 생성
            User tUser1 = User.builder()
                    .loginId("teacher01")
                    .password(passwordEncoder.encode("test1234!"))
                    .name("김수학")
                    .gender(Gender.FEMALE)
                    .role(UserRole.TEACHER)
                    .status(UserStatus.ACTIVE)
                    .build();
            em.persist(tUser1);

            // 선생님 1 꼬리(Teacher) 생성
            teacher1 = Teacher.builder()
                    .user(tUser1)
                    .employeeNumber("EMP2026001")
                    .subject(mathSubject)
                    .officeLocation("본관 2층 제1교무실")
                    .officePhoneNumber("02-123-4567")
                    .employmentStatus(EmploymentStatus.ACTIVE)
                    .build();
            em.persist(teacher1);

            // 선생님 2 몸통(User) 생성
            User tUser2 = User.builder()
                    .loginId("teacher02")
                    .password(passwordEncoder.encode("test1234!"))
                    .name("박국어")
                    .gender(Gender.MALE)
                    .role(UserRole.TEACHER)
                    .status(UserStatus.ACTIVE)
                    .build();
            em.persist(tUser2);

            // 선생님 2 꼬리(Teacher) 생성
            teacher2 = Teacher.builder()
                    .user(tUser2)
                    .employeeNumber("EMP2026002")
                    .subject(korSubject)
                    .officeLocation("본관 2층 제2교무실")
                    .officePhoneNumber("02-123-4568")
                    .employmentStatus(EmploymentStatus.ACTIVE)
                    .build();
            em.persist(teacher2);

            // 선생님 3 몸통(User) 생성 (1학년 4반 담임 & 수학)
            User tUser3 = User.builder()
                    .loginId("teacher3")
                    .password(passwordEncoder.encode("test1234"))
                    .name("최수학")
                    .gender(Gender.MALE)
                    .role(UserRole.TEACHER)
                    .status(UserStatus.ACTIVE)
                    .build();
            em.persist(tUser3);

            // 선생님 3 꼬리(Teacher) 생성
            teacher3 = Teacher.builder()
                    .user(tUser3)
                    .employeeNumber("EMP2026003")
                    .subject(mathSubject)
                    .officeLocation("본관 2층 제3교무실")
                    .officePhoneNumber("02-123-4569")
                    .employmentStatus(EmploymentStatus.ACTIVE)
                    .build();
            em.persist(teacher3);

            System.out.println("🍠 [춘식이 알림] 더미 선생님 데이터(김수학, 박국어, 최수학) 맹글었슈!");
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

            // 1학년 3반 (홍길동 전학용 - 일단 담임 없이)
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

            System.out.println("🍠 [춘식이 알림] 학급 생성 및 담임 선생님 배정 완료했슈!");

            // ==========================================
            // 4. 초기 테스트용 더미 학생 및 소속/초대장 생성 (3반 소속)
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

            System.out.println("🍠 [춘식이 알림] 1학년 3반 15번 홍길동 데이터까지 싹 다 넣었슈!");

            // ==========================================
            // 5. 1학년 4반 더미 학생 20명 생성
            // ==========================================
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
            }
            System.out.println("🍠 [춘식이 알림] 1학년 4반 20명 학생 데이터 맹글었슈!");

            // ==========================================
            // 6. 1학년 2반 더미 학생 10명 생성
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
            System.out.println("🍠 [춘식이 알림] 1학년 2반 10명 학생 데이터 맹글었슈!");

            // ==========================================
            // 7. 과목 배정(SubjectAssignment) 생성
            // ==========================================
            // 최수학(teacher3) 선생님을 1학년 4반(class4) 수학(mathSubject) 담당으로 배정
            SubjectAssignment assignment1 = SubjectAssignment.builder()
                    .teacher(teacher3)
                    .classroom(class4)
                    .subject(mathSubject)
                    .academicYear(2026)
                    .semester(1)
                    .build();
            em.persist(assignment1);

            // 최수학(teacher3) 선생님을 1학년 2반(class2) 수학(mathSubject) 담당으로 배정
            SubjectAssignment assignment2 = SubjectAssignment.builder()
                    .teacher(teacher3)
                    .classroom(class2)
                    .subject(mathSubject)
                    .academicYear(2026)
                    .semester(1)
                    .build();
            em.persist(assignment2);

            // 김수학(teacher1) 선생님을 1학년 1반(class1) 수학(mathSubject) 담당으로 배정
            SubjectAssignment assignment3 = SubjectAssignment.builder()
                    .teacher(teacher1)
                    .classroom(class1)
                    .subject(mathSubject)
                    .academicYear(2026)
                    .semester(1)
                    .build();
            em.persist(assignment3);

            // 박국어(teacher2) 선생님을 1학년 2반(class2) 국어(korSubject) 담당으로 배정
            SubjectAssignment assignment4 = SubjectAssignment.builder()
                    .teacher(teacher2)
                    .classroom(class2)
                    .subject(korSubject)
                    .academicYear(2026)
                    .semester(1)
                    .build();
            em.persist(assignment4);

            System.out.println("🍠 [춘식이 알림] 모든 선생님의 과목 배정(SubjectAssignment) 완료!");
        } else {
            System.out.println("🍠 [춘식이 알림] 2026학년도 학급 데이터가 이미 있어서 스킵합니더.");
        }
    }
}