package com.fastcampus.projectboard.dto.security;

import com.fastcampus.projectboard.dto.UserAccountDto;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// 로그인 유저의 정보를 가져오는 dto
public record BoardPrincipal(
        String username,
        String password,
        Collection<? extends GrantedAuthority> authorities,         // 권한 종류 모음
        String email,
        String nickname,
        String memo
) implements UserDetails{       // UserDetails 클래스 상속받아 사용 (이미 만들어져 있는 클래스로 유저에 관해 기본 값들을 반환해주는 메서드들의 집합)

    public static BoardPrincipal of(String username, String password, String email, String nickname, String memo) {
        // 지금은 인증만 하고 권한을 다루고 있지 않아서 임의로 세팅한다.
        Set<RoleType> roleTypes = Set.of(RoleType.USER);

        return new BoardPrincipal(
                username,
                password,
                roleTypes.stream()
                        .map(RoleType::getName)     // RoleType에서 권한 종류 가져옴
                        .map(SimpleGrantedAuthority::new)   // 이미 만들어진 GrantedAuthority의 기본 구현체인 SimpleGrantedAuthority를 통해 권한정보 생성함
                        .collect(Collectors.toUnmodifiableSet())
                ,
                email,
                nickname,
                memo
        );
    }

    // UserAccoumtDto -> BoardPrincipal
    public static BoardPrincipal from(UserAccountDto dto) {
        return BoardPrincipal.of(
                dto.userId(),
                dto.userPassword(),
                dto.email(),
                dto.nickname(),
                dto.memo()
        );
    }

    // BoardPrincipal ->  UserAccoumtDto
    public UserAccountDto toDto() {
        return UserAccountDto.of(
                username,
                password,
                email,
                nickname,
                memo
        );
    }

    // UserDetails에서 제공하는 기본 메서드 사용

    @Override public String getUsername() { return username; }
    @Override public String getPassword() { return password; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }        // 권한 부여 부분

    // UserDetails에서 제공하는 기본 메서드 중 필요없는 메서드는 return 값을 true로 하여 상관없이 만듦
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }

    
    
    // 유저 권한 종류
    public enum RoleType {
        USER("ROLE_USER");

        @Getter private final String name;

        RoleType(String name) {
            this.name = name;
        }
    }

}
