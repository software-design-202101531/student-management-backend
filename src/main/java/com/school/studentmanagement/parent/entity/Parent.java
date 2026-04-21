package com.school.studentmanagement.parent.entity;

import com.school.studentmanagement.global.enums.RelationType;
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
public class Parent {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
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
