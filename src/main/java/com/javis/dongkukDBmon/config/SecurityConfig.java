package com.javis.dongkukDBmon.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig implements WebMvcConfigurer {

    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (API 서버일 경우 추천)
                .csrf(AbstractHttpConfigurer::disable)
                // CORS 설정 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .headers(h -> h
                        // ① 레거시 헤더: 같은 출처에서만 iframe 허용
                        .frameOptions(fo -> fo.sameOrigin())
                        // ② 현대 브라우저용: 이 응답을 iframe으로 '누가' 담을 수 있는지 지정
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "frame-ancestors 'self' http://10.90.4.60:8812 http://172.31.1.176:8813"
                                )
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        // [공개 경로]
                        .requestMatchers(
                                "/", "/index.html",                // 메인 페이지
                                "/favicon.ico",                    // 파비콘
                                "/static/**", "/assets/**",        // 정적 리소스
                                "/fonts/**", "/js/**", "/css/**",  // 리소스
                                "/paper-boot.html",     // paper boot
                                "/paper/**",
                                // API 전체 허용(하위 경로 포함)
                                "/api/**",
                                // WebSocket
                                "/ws/**",
                                // ===== 업무 화면 =====
                                "/db-list", "/sms-history", "/threshold-list", "/tablespaces", "/dailyChk", "/SysInfoDetail", "/TEST",
                                // ETL 관련
                                "/ETLJobList", "/etljob-history",
                                "/ETLjob-Scheduler","/ManagerMenuRole",
                                //Password 관련
                                "/reset-password","/reset-password/**"
                        ).permitAll()
                        // [그 외에는 인증 필요]
                        .anyRequest().authenticated()
                )
                // JWT 인증 필터 적용
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://10.90.4.60:8812",
                "http://172.31.1.176:8813"
        ));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }


}

