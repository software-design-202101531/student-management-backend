package com.school.studentmanagement.global.controller;

import com.school.studentmanagement.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// 배포 후 생존 확인용 헬스체크. 인증 없이 접근 가능(SecurityConfig permitAll).
// DB 등 의존성은 확인하지 않고, 애플리케이션이 HTTP 요청을 처리 중인지만 알린다.
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("status", "UP")));
    }
}
