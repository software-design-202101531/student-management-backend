package com.school.studentmanagement.user.repository;

import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginId(String loginId);

    boolean existsByRole(UserRole role);
}
