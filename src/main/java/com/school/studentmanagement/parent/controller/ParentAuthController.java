package com.school.studentmanagement.parent.controller;

import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.parent.dto.ParentRegisterRequest;
import com.school.studentmanagement.parent.dto.VerifyParentRequest;
import com.school.studentmanagement.parent.service.ParentAuthService;
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

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Long>> verifyParent(@RequestBody VerifyParentRequest request) {
        Long invitationId = parentAuthService.verifyParent(request);
        return ResponseEntity.ok(ApiResponse.ok(invitationId));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> registerParent(@RequestBody ParentRegisterRequest request) {
        parentAuthService.registerParent(request);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
