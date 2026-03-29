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
import org.springframework.security.crypto.password.PasswordEncoder; // ✨ 암호화용 추가
import org.springframework.transaction.annotation.Transactional;

@Configuration
@RequiredArgsConstructor
public class InitDataConfig implements CommandLineRunner {

    private final EntityManager em;
    // 실무 포인트: 관리자 비밀번호는 꼭 암호화해서 넣어야 혀요!
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        // ==========================================
        // ✨ 0. 최고 관리자(Super Admin) 계정 생성
        // ==========================================
        // 실무 포인트: 서버 껐다 켤 때마다 중복 생성되지 않도록 카운트 조회 방어 로직!
        Long adminCount = em.createQuery("SELECT count(u) FROM User u WHERE u.role = :role", Long.class)
                .setParameter("role", UserRole.ADMIN) // UserRole enum에 SUPER_ADMIN이 있다고 가정해유
                .getSingleResult();

        if (adminCount == 0) {
            User adminUser = User.builder()
                    .name("최고관리자")
                    .loginId("admin")
                    .password(passwordEncoder.encode("admin1234!")) // 현업에선 무조건 암호화 필수당께요!
                    .role(UserRole.ADMIN)
                    .status(UserStatus.ACTIVE) // 관리자는 바로 활성 상태로
                    .build();
            em.persist(adminUser);
            System.out.println("🍠 [춘식이 알림] 최고 관리자 계정(admin1234!) 뚝딱 맹글었슈!");
        } else {
            System.out.println("🍠 [춘식이 알림] 최고 관리자 계정이 이미 있어서 걍 넘어갑니더.");
        }

        // ==========================================
        // 1. 학급 생성 (2026년 1학년 1반)
        // ==========================================
        Classroom classroom = Classroom.builder()
                .academicYear(2026)
                .semester(1)
                .grade(1)
                .classNum(1)
                .build();
        em.persist(classroom);

        // ==========================================
        // 2. 공통 User 생성 (홍길동, 대기 상태)
        // ==========================================
        User user = User.builder()
                .name("홍길동")
                .password("dummy_password") // 아직 미가입 상태라 더미 비밀번호
                .role(UserRole.STUDENT)
                .status(UserStatus.PENDING)
                .build();
        em.persist(user);

        // ==========================================
        // 3. Student 상세 생성 (입학년도 2026)
        // ==========================================
        Student student = Student.builder()
                .user(user)
                .enrollmentYear(2026)
                .build();
        em.persist(student);

        // ==========================================
        // 4. 소속 이력 매핑 (15번)
        // ==========================================
        StudentAffiliation affiliation = StudentAffiliation.builder()
                .student(student)
                .classroom(classroom)
                .studentNum(15)
                .build();
        em.persist(affiliation);

        // ==========================================
        // 5. ✨ 학부모 가입 대기용 초대장(ParentInvitation) 생성
        // ==========================================
        ParentInvitation invitation = ParentInvitation.builder()
                .student(student)
                .phoneNumber("010-1234-5678")
                .relationType(RelationType.FATHER)
                .build();
        em.persist(invitation);

        System.out.println("🍠 [춘식이 알림] 1학년 1반 15번 홍길동 데이터 및 학부모(010-1234-5678) 초대장 삽입 완벽하게 성공했습니더!");
    }
}