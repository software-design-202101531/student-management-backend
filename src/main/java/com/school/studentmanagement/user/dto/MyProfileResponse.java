package com.school.studentmanagement.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.school.studentmanagement.global.enums.EmploymentStatus;
import com.school.studentmanagement.global.enums.Gender;
import com.school.studentmanagement.global.enums.RelationType;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.user.entity.User;

/**
 * 로그인한 본인의 프로필 응답 (GET /api/me).
 * 공통 필드(이름·성별·역할·사진)는 항상 채우고, 역할별 상세는 해당 역할만 채운다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MyProfileResponse( // 공통역할 + 상세 역할 조합
        Long id,
        String name,
        Gender gender,
        UserRole role,
        String profileImageUrl,     // 학생/교사만, 미등록 시 null (학부모는 항상 null)
        StudentDetail student,
        ParentDetail parent,
        TeacherDetail teacher
) {

    // 각 역할별 상세 역할
    public record StudentDetail(
            String address,
            String phoneNumber,
            Integer enrollmentYear,
            Integer grade,                // 현재 학년도/학기 배정 반 기준 (미배정 시 null)
            Integer classNum,
            String homeroomTeacherName    // 담임 미배정 시 null
    ) {}

    public record ParentDetail(
            String phoneNumber,
            RelationType relationType
    ) {}

    public record TeacherDetail(
            String employeeNumber,
            String subjectName,
            String officeLocation,
            String officePhoneNumber,
            EmploymentStatus employmentStatus
    ) {}

    public static MyProfileResponse ofStudent(User user, String profileImageUrl, StudentDetail student) {
        return new MyProfileResponse(user.getId(), user.getName(), user.getGender(), user.getRole(),
                profileImageUrl, student, null, null);
    }

    public static MyProfileResponse ofParent(User user, ParentDetail parent) {
        return new MyProfileResponse(user.getId(), user.getName(), user.getGender(), user.getRole(),
                null, null, parent, null);
    }

    public static MyProfileResponse ofTeacher(User user, String profileImageUrl, TeacherDetail teacher) {
        return new MyProfileResponse(user.getId(), user.getName(), user.getGender(), user.getRole(),
                profileImageUrl, null, null, teacher);
    }

    public static MyProfileResponse ofAdmin(User user) {
        return new MyProfileResponse(user.getId(), user.getName(), user.getGender(), user.getRole(),
                null, null, null, null);
    }
}
