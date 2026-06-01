package com.school.studentmanagement.global.config;

import com.school.studentmanagement.global.enums.Gender;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.user.entity.User;
import com.school.studentmanagement.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 운영(prod) 전용 최초 관리자 부트스트랩.
 *
 * 운영은 데모 시딩이 기본 비활성(DEMO_SEED_ENABLED=false)이라 자동 생성되는 계정이 없다.
 * → prod 첫 배포 시 로그인 가능한 ADMIN 이 0명이 되는 문제를 메운다.
 * (DEMO_SEED_ENABLED=true 로 데모를 켜면 DemoDataSeeder 가 admin 을 만들 수도 있으나, 둘 다 ADMIN 0명일 때만 동작하여 멱등하다.)
 *
 * 동작: ADMIN 이 하나도 없을 때만, 환경변수(ADMIN_LOGIN_ID / ADMIN_PASSWORD)로 1명 생성(멱등).
 *  - 자격증명이 비어 있으면 생성하지 않고 경고만 남긴다(운영자가 인지하도록).
 *  - 이미 ADMIN 이 있으면 아무것도 하지 않는다.
 *
 * 보안: 비밀번호는 PasswordEncoder 로 해시. 평문/해시를 로그에 남기지 않는다.
 *       최초 1회 부트스트랩 후 즉시 비밀번호 변경을 권장(로그에 안내).
 */
@Slf4j
@Configuration
@Profile("prod")
@RequiredArgsConstructor
public class ProdAdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.login-id:}")
    private String adminLoginId;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.name:운영관리자}")
    private String adminName;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            return; // 이미 관리자가 있으면 부트스트랩 불필요
        }

        if (!StringUtils.hasText(adminLoginId) || !StringUtils.hasText(adminPassword)) {
            log.warn("[운영 부트스트랩] ADMIN 계정이 없는데 ADMIN_LOGIN_ID/ADMIN_PASSWORD 가 비어 있어 관리자를 생성하지 못했습니다. "
                    + "환경변수를 설정해 재기동하면 최초 관리자가 생성됩니다.");
            return;
        }

        User admin = User.createActive(
                adminLoginId, passwordEncoder.encode(adminPassword),
                adminName, Gender.MALE, UserRole.ADMIN);
        userRepository.save(admin);

        log.warn("[운영 부트스트랩] 최초 ADMIN 계정(loginId={})을 생성했습니다. 즉시 비밀번호를 변경하세요.", adminLoginId);
    }
}
