package com.school.studentmanagement.record.controller;

import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import com.school.studentmanagement.record.service.SubjectRecordService;
import com.school.studentmanagement.subject.dto.SubjectRecordRequest;
import com.school.studentmanagement.subject.dto.SubjectRecordResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/classrooms/{classroomId}/subjects/{subjectId}/students/{studentId}/records")
@RequiredArgsConstructor
public class SubjectRecordController {

    private final SubjectRecordService subjectRecordService;

    // 과세특 상세 조회 API
    @GetMapping
    public ResponseEntity<SubjectRecordResponse> getSubjectRecord(
            @PathVariable("classroomId") Long classroomId,
            @PathVariable("subjectId") Long subjectId,
            @PathVariable("studentId") Long studentId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        // 토큰에서 userId 추출
        Long teacherId = customUserDetails.getUserId();

        SubjectRecordResponse response = subjectRecordService.getSubjectRecord(classroomId, subjectId, studentId, teacherId);

        return ResponseEntity.ok(response);
    }

    // 과세특 저장 및 수정
    public ResponseEntity<Void> saveSubjectRecord(
            @PathVariable("classroomId") Long classroomId,
            @PathVariable("subjectId") Long subjectId,
            @PathVariable("studentId") Long studentId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody SubjectRecordRequest request
    ) {
        // 토큰에서 userId 추출
        Long teacherId = customUserDetails.getUserId();

        subjectRecordService.saveSubjectRecord(classroomId, subjectId, studentId, teacherId, request);

        // 200 OK 상태코드 리턴
        return ResponseEntity.ok().build();
    }
}
