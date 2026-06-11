package com.school.studentmanagement.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing(BaseTimeEntity의 createdAt/updatedAt) 활성화.
 *
 * <p>메인 애플리케이션 클래스가 아니라 별도 @Configuration 으로 둔다 — @WebMvcTest 같은 웹 슬라이스
 * 컨텍스트는 이 빈을 로드하지 않으므로, JPA 메타모델이 없는 슬라이스에서 jpaAuditingHandler 생성 실패
 * ("JPA metamodel must not be empty")를 피한다. 전체 컨텍스트(@SpringBootTest)는 정상 스캔되어 동작한다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
