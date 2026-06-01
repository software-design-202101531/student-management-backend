package com.school.studentmanagement.student.service;

import com.school.studentmanagement.global.dto.ProfileImageResponse;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.global.storage.ProfileImageManager;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class StudentProfileImageService {

    private static final String KEY_PREFIX = "profiles/students";

    private final StudentRepository studentRepository;
    private final ProfileImageManager profileImageManager;

    /**
     * 담임 교사가 자기 반 학생의 프로필 사진을 등록/수정한다.
     */
    @Transactional
    public ProfileImageResponse updateProfileImage(Long studentId, Long teacherId, MultipartFile file) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));

        if (!student.isHomeroomTeacher(teacherId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "담임을 맡은 반 학생만 수정할 수 있습니다");
        }

        return profileImageManager.replace(
                file, KEY_PREFIX, student.getProfileImageKey(), student::updateProfileImageKey);
    }
}
