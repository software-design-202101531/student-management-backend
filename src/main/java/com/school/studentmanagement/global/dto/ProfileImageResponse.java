package com.school.studentmanagement.global.dto;

// 프로필 사진 업로드/수정 결과. imageUrl 은 조회용 presigned URL.
public record ProfileImageResponse(String imageUrl) {
}
