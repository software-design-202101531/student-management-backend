// 📍 추천 위치: src/main/java/com/school/studentmanagement/global/config/InitDataConfig.java
// 💡 추천 이유: 프로젝트 구동 시 기초 데이터를 세팅하는 환경 설정 파일이므로 global/config 하위가 적절해유.

package com.school.studentmanagement.global.config;

import com.school.studentmanagement.affiliation.entity.StudentAffiliation;
import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.user.entity.Student;
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
        // 0. 최고 관리자 계정 생성 (방어 로직 포함)
        // ==========================================
        Long adminCount = em.createQuery("SELECT count(u) FROM User u WHERE u.role = :role", Long.class)
                .setParameter("role", UserRole.ADMIN)
                .getSingleResult();

        if (adminCount == 0) {
            User adminUser = User.builder()
                    .name("최고관리자")
                    .loginId("admin")
                    .password(passwordEncoder.encode("admin1234!"))
                    .role(UserRole.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .build();
            em.persist(adminUser);
            System.out.println("🍠 [춘식이 알림] 최고 관리자 계정(admin1234!) 뚝딱 맹글었슈!");
        }

        // ==========================================
        // 1. 학급 생성 (2026년 1학년 1반, 2반, 3반)
        // ==========================================
        Long classCount = em.createQuery("SELECT count(c) FROM Classroom c WHERE c.academicYear = 2026 AND c.grade = 1", Long.class)
                .getSingleResult();

        if (classCount == 0) {
            // 1학년 1반 생성
            Classroom class1 = Classroom.builder()
                    .academicYear(2026)
                    .semester(1)
                    .grade(1)
                    .classNum(1)
                    .build();
            em.persist(class1);

            // 1학년 2반 생성
            Classroom class2 = Classroom.builder()
                    .academicYear(2026)
                    .semester(1)
                    .grade(1)
                    .classNum(2)
                    .build();
            em.persist(class2);

            // ✨ 1학년 3반 생성 (홍길동 전학용)
            Classroom class3 = Classroom.builder()
                    .academicYear(2026)
                    .semester(1)
                    .grade(1)
                    .classNum(3)
                    .build();
            em.persist(class3);

            System.out.println("🍠 [춘식이 알림] 2026학년도 1학년 1반, 2반, 3반 기초 데이터 세팅 완료했슈!");

            // ==========================================
            // 2. 초기 테스트용 더미 학생 및 소속/초대장 생성 (3반 소속으로)
            // ==========================================
            User user = User.builder()
                    .name("홍길동")
                    .role(UserRole.STUDENT)
                    .status(UserStatus.PENDING)
                    .build();
            em.persist(user);

            Student student = Student.builder()
                    .user(user)
                    .enrollmentYear(2026)
                    .build();
            em.persist(student);

            // ✨ 소속을 3반(class3)으로 매핑
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

            System.out.println("🍠 [춘식이 알림] 1학년 3반 15번 홍길동 데이터 및 초대장까지 싹 다 넣었슈!");
        } else {
            System.out.println("🍠 [춘식이 알림] 1학년 반 데이터가 이미 있어서 스킵합니더.");
        }
    }
}