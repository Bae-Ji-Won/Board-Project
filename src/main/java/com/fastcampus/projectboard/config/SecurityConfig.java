package com.fastcampus.projectboard.config;

import com.fastcampus.projectboard.domain.UserAccount;
import com.fastcampus.projectboard.dto.security.BoardPrincipal;
import com.fastcampus.projectboard.dto.security.KakaoOAuth2Response;
import com.fastcampus.projectboard.service.UserAccountService;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;

import java.util.UUID;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService
    )throws Exception {
        return http
                // 기존 방식
                // .authorizeHttpRequests()
                // .mvcMatchers("/").permitAll()
                //  .and()
                // 람다식 변경을 통해 .and()를 사용하지 않고 구분이 명확해짐
                .authorizeHttpRequests(auth -> auth
                        // requestMatchers(PathRequest.toStaticResources().atCommonLocations()) => 이미 만들어진 기본적인 정적 리소스 파일들 위치
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll() // 기본적인 정적 리소스에 대한 Security check 비활성화
                        .mvcMatchers("/api/**").permitAll()
                        .mvcMatchers(
                                HttpMethod.GET,
                                "/",
                                "/articles",
                                "/articles/search-hashtag"
                        ).permitAll()           // 해당 내용의 api 접근 허용
                        .anyRequest().authenticated()       //  그 외의 요청은 모두 인증 필요
                )
                // 기존 방식
                // .formLogin()
                // .and()
                // withDefaults() => 아무런 일도 하지 않음
                .formLogin(withDefaults())
                // 기존 방식
                // .logout()
                //      .logoutSuccessUrl("/")
                //      .and()
                .logout(logout -> logout.logoutSuccessUrl("/"))
                .csrf(csrf -> csrf.ignoringAntMatchers("/api/**"))
                .oauth2Login(oAuth -> oAuth
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oAuth2UserService)))
                .build();
    }

    @Bean
    public UserDetailsService userDetailsService(UserAccountService userAccountService) {
        return username -> userAccountService
                .searchUser(username)
                .map(BoardPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("유저를 찾을 수 없습니다 - username: " + username));
    }

    // 카카오톡에서 데이터를 받아와 인증하기 위한 작업
    // 기존 OAuth2UserService파일말고 직접 Bean파일을 만들어서 가공함
    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService(
            UserAccountService userAccountService,
            PasswordEncoder passwordEncoder
    ){
        // Spring Security 공식 문서에 OAuth 사용법
        final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

        return userRequest -> {
            OAuth2User oAuth2User = delegate.loadUser(userRequest);

            KakaoOAuth2Response kakaoResponse = KakaoOAuth2Response.from(oAuth2User.getAttributes());

            // 카카오에서 보내주는 데이터중 고유값을 만들어내 해당 유저의 정보 DB 존재 여부 확인
            String registrationId = userRequest.getClientRegistration().getRegistrationId();    // 고유값인 client ID를 추출할 수 있음
            String providerId = String.valueOf(kakaoResponse.id());
            String username = registrationId + "_" + providerId;                        // 카카오톡에서는 username이 없기 때문에 보내준 다른 PK 정보를 가지고 username을 임의로 생성해줌
            String dummyPassword = passwordEncoder.encode("{bcrypt}"+UUID.randomUUID());     // 비밀번호 임의 생성(카카오톡에서는 아이디와 비밀번호는 제공해 주지 않음) , UUID 랜덤 값

            // searchUser를 통해 카카오톡에서 보내준 정보의 유저가 존재하는지 확인
            // 존재한다면 BoardPrincipal::from를 통해 BoardPrincipal DTO에 저장
            // 존재하지않다면 DB에 새로 저장(회원가입 유도)
            return userAccountService.searchUser(username)
                    .map(BoardPrincipal::from)
                    .orElseGet(() ->
                            BoardPrincipal.from(
                                    userAccountService.saveUser(
                                            username,
                                            dummyPassword,
                                            kakaoResponse.email(),
                                            kakaoResponse.nickname(),
                                            null
                                    )
                            )
                    );
        };
    }

    // SpringSecurity 암호화 모듈 사용
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
