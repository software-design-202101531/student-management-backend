package com.school.studentmanagement.grade.controller;

import com.school.studentmanagement.global.enums.ExamType;
import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import com.school.studentmanagement.grade.dto.ClassroomGradeResponse;
import com.school.studentmanagement.grade.dto.GradeListResponse;
import com.school.studentmanagement.grade.dto.GradeSaveRequest;
import com.school.studentmanagement.grade.dto.GradeUpdateRequest;
import com.school.studentmanagement.grade.service.StudentGradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/classrooms/{classroomId}")
@RequiredArgsConstructor
public class StudentGradeController {

    private final StudentGradeService studentGradeService;

    // 과목 담당 교사: 학급 전체 성적 일괄 입력
    @PostMapping("/subjects/{subjectId}/grades")
    public ResponseEntity<ApiResponse<Void>> saveGrades(
            @PathVariable Long classroomId,
            @PathVariable Long subjectId,
            @RequestBody @Valid GradeSaveRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        studentGradeService.saveGrades(classroomId, subjectId, userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    // 과목 담당 교사: 학생 개별 성적 수정
    @PutMapping("/subjects/{subjectId}/grades/{gradeId}")
    public ResponseEntity<ApiResponse<Void>> updateGrade(
            @PathVariable Long classroomId,
            @PathVariable Long subjectId,
            @PathVariable Long gradeId,
            @RequestBody @Valid GradeUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        studentGradeService.updateGrade(classroomId, subjectId, gradeId, userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    // 과목 담당 교사: 담당 과목 성적 조회
    @GetMapping("/subjects/{subjectId}/grades")
    public ResponseEntity<ApiResponse<GradeListResponse>> getSubjectGrades(
            @PathVariable Long classroomId,
            @PathVariable Long subjectId,
            @RequestParam Integer academicYear,
            @RequestParam Integer semester,
            @RequestParam ExamType examType,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        GradeListResponse response = studentGradeService.getSubjectGrades(
                classroomId, subjectId, userDetails.getUserId(), academicYear, semester, examType);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // 담임 교사: 담당 학급 전 과목 성적 조회
    @GetMapping("/grades")
    public ResponseEntity<ApiResponse<ClassroomGradeResponse>> getClassroomGrades(
            @PathVariable Long classroomId,
            @RequestParam Integer academicYear,
            @RequestParam Integer semester,
            @RequestParam ExamType examType,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ClassroomGradeResponse response = studentGradeService.getClassroomGrades(
                classroomId, userDetails.getUserId(), academicYear, semester, examType);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
