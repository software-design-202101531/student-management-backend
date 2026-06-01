package com.school.studentmanagement.teacher.service;

import com.school.studentmanagement.global.dto.ProfileImageResponse;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.global.storage.ProfileImageManager;
import com.school.studentmanagement.teacher.entity.Teacher;
import com.school.studentmanagement.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class TeacherProfileImageService {

    private static final String KEY_PREFIX = "profiles/teachers";

    private final TeacherRepository teacherRepository;
    private final ProfileImageManager profileImageManager;

    /**
     * 관리자가 교사의 프로필 사진을 등록/수정한다. (ADMIN 권한은 경로(/api/admin/**)에서 강제)
     */
    @Transactional
    public ProfileImageResponse updateProfileImage(Long teacherId, MultipartFile file) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_NOT_FOUND));

        return profileImageManager.replace(
                file, KEY_PREFIX, teacher.getProfileImageKey(), teacher::updateProfileImageKey);
    }
}
