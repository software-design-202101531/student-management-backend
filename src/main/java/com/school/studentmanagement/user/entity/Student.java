package com.school.studentmanagement.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "students")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Student {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private Integer enrollmentYear;


    @Builder
    public Student(Long id, User user, Integer enrollmentYear) {
        this.id = id;
        this.user = user;
        this.enrollmentYear = enrollmentYear;
    }
}
