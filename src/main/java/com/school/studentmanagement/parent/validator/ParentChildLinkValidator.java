package com.school.studentmanagement.parent.validator;

import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.parent.repository.ParentStudentMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 학부모-자녀 연결 여부를 검증하는 공통 컴포넌트.
 * 학부모가 본인과 연결된 자녀의 정보에만 접근하도록 보장한다.
 */
@Component
@RequiredArgsConstructor
public class ParentChildLinkValidator {

    private final ParentStudentMappingRepository mappingRepository;

    public void validateLinked(Long parentId, Long studentId) {
        if (!mappingRepository.existsByParentIdAndStudentId(parentId, studentId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "연결된 자녀가 아닙니다");
        }
    }
}
