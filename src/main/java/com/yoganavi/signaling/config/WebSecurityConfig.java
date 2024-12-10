package com.yoganavi.signaling.config;

import com.yoganavi.signaling.filter.UserAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


/*
*
*
* WebSocket의 경우
* Handshake 단계에서만 HTTP 헤더를 확인할 수 있음
* 연결이 수립된 후에는 HTTP 헤더를 사용할 수 없음
*
* */

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    private final UserAuthenticationFilter userAuthenticationFilter;

    public WebSecurityConfig(UserAuthenticationFilter userAuthenticationFilter) {
        this.userAuthenticationFilter = userAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/signaling/rtc").hasAnyRole("USER", "TEACHER")
                .anyRequest().authenticated()
            )
            .addFilterBefore(userAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}