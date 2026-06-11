package com.school.studentmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableJpaAuditing 은 JpaAuditingConfig 로 분리(웹 슬라이스 테스트 호환). [[global/config/JpaAuditingConfig]]
@SpringBootApplication
@EnableScheduling
public class StudentManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudentManagementApplication.class, args);
    }

}
