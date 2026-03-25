package com.school.studentmanagement.user.controller;


import com.school.studentmanagement.user.dto.ActivateAccountRequest;
import com.school.studentmanagement.user.dto.VerifyStudentRequest;
import com.school.studentmanagement.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/verify-student")
    public ResponseEntity<Long> verifyStudent(@Valid @RequestBody VerifyStudentRequest request) {
        Long UserId = userService.verifyStudent(request);

        return ResponseEntity.ok(UserId);
    }

    @PostMapping("/activate")
    public ResponseEntity<String> activateAccount(@Valid @RequestBody ActivateAccountRequest request) {
        userService.activateAccount(request);

        return ResponseEntity.ok("학생 계정 활성화 완료");
    }
}
