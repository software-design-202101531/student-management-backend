package com.school.studentmanagement.parent.entity;

import com.school.studentmanagement.global.entity.BaseTimeEntity;
import com.school.studentmanagement.global.enums.RelationType;
import com.school.studentmanagement.global.security.encryption.AesRandomStringConverter;
import com.school.studentmanagement.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "parents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Parent extends BaseTimeEntity {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    // PII — 컬럼 레벨 암호화(AES, 랜덤 IV). 검색 미사용.
    @Convert(converter = AesRandomStringConverter.class)
    @Column(nullable = false, length = 255)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RelationType relationType;

    @Builder
    private Parent(User user, String phoneNumber, RelationType relationType) {
        this.user = user;
        this.phoneNumber = phoneNumber;
        this.relationType = relationType;
    }
}
