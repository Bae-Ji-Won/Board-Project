package com.fastcampus.projectboard.config;

import com.fastcampus.projectboard.dto.security.BoardPrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@EnableJpaAuditing
@Configuration
public class JpaConfig {

    // 원래는 Cotroller에서 SecurityContextHolder를 호출해서 현재 로그인한 유저의 정보를 가져옴
    // 하지만 AuditorAware에서 미리 먼저 유저의 정보를 가져와 BoardPrincipal 클래스에 유저 정보를 넣어줌 그 후, Controller에서 해당 정보를 가져다 사용함
    // SpringSecurity에 저장되어 있는 회원의 정보를 가져옴
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext())    // SecurityContextHolder 인증정보 모두 가지고 있는 곳
                .map(SecurityContext::getAuthentication)                        // Authentication 정보 호출
                .filter(Authentication::isAuthenticated)                        // 호출한 Authentication 정보가 isAuthenticated(인증)이 되었는지 확인
                .map(Authentication::getPrincipal)                              // Authentication의 getPrincipal 정보 호출
                .map(BoardPrincipal.class::cast)                                // 사용자가 만든 BoardPrincipal클래스에 Principal 정보 cast
                .map(BoardPrincipal::getUsername);                              // BoardPrincipal에 현재 로그인한 유저의 정보 대입
    }
}
