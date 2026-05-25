package com.school.studentmanagement.grade.service;

import com.school.studentmanagement.global.enums.GradeLevel;
import com.school.studentmanagement.grade.repository.GradePolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradePolicyService {

    private final GradePolicyRepository gradePolicyRepository;

    // 활성 정책으로 평가. 정책이 없으면 enum 기본 컷오프(GradeLevel.from)를 사용한다.
    public GradeLevel evaluate(double averageScore) {
        return gradePolicyRepository.findFirstByActiveTrue()
                .map(p -> p.evaluate(averageScore))
                .orElseGet(() -> GradeLevel.from(averageScore));
    }
}
