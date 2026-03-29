package com.school.studentmanagement.global.security.dto;

import com.school.studentmanagement.global.enums.UserStatus;
import com.school.studentmanagement.user.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final User user;

    // 유저 권한 반환 메서드
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
    }

    // 유저 비밀번호 반환 메서드
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    // 유저 pk 반환 메서드
    @Override
    public String getUsername() {
        return String.valueOf(user.getId());
    }


    // 계정 상태 검사 로직
    // 상태가 UserStatus.ACTIVE 상태일 때만 로그인과 권한을 허용
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() == UserStatus.ACTIVE;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == UserStatus.ACTIVE;
    }
}
