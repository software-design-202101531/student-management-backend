package com.school.studentmanagement.invitation.controller;


import com.school.studentmanagement.invitation.dto.ParentRegisterRequest;
import com.school.studentmanagement.invitation.dto.ParentVerifyRequest;
import com.school.studentmanagement.invitation.service.ParentAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parents")
@RequiredArgsConstructor
public class ParentAuthController {

    private final ParentAuthService parentAuthService;

    // 가입 가능 여부 검증 컨트롤러
    @PostMapping("/verify")
    public ResponseEntity<Long> verifyParent(@RequestBody ParentVerifyRequest request) {
        Long invitationId = parentAuthService.verifyParent(request);

        return ResponseEntity.ok(invitationId);
    }

    // 최종 회원가입 컨트롤러
    @PostMapping("/register")
    public ResponseEntity<String> registerParent(@RequestBody ParentRegisterRequest request) {
        parentAuthService.registerParent(request);

        return ResponseEntity.ok("학부모님의 회원가입이 완료되었습니다");
    }
}
