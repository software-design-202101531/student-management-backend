package com.school.studentmanagement.student.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 교사용 학생 학생부 기본 프로필. 현재 학기 학급 정보 + 학생 상세 + 담임 이름.
 */
@Getter
@Builder
public class StudentProfileResponse {
    private Long studentId;
    private String name;
    private Integer grade;              // 현재 학기 학년 (학급 미배정 시 null)
    private Integer classNum;           // 현재 학기 반
    private Integer studentNum;         // 출석 번호
    private String address;
    private String phoneNumber;
    private String profileImageUrl;     // presigned URL (없으면 null)
    private Integer enrollmentYear;
    private String homeroomTeacherName; // 담임 미배정 시 null
}
