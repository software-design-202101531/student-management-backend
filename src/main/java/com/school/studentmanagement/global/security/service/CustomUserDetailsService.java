package com.school.studentmanagement.global.security.service;

import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import com.school.studentmanagement.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String userIdStr) throws UsernameNotFoundException {
        Long userId;
        try {
            // String으로 넘어온 pk를 Long으로 변환
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException("유효하지 않은 사용자 입니다");
        }
        // loginId가 아니라 PK로 유저를 찾는다
        return userRepository.findById(userId)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("유효한 사용자가 아닙니다"));
    }
}
