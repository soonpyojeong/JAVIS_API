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
                .authorizeHttpRequests(auth -> auth
                        // [공개 경로]
                        .requestMatchers(
                                "/", "/index.html",                // 메인 페이지
                                "/favicon.ico",                    // 파비콘
                                "/static/**", "/assets/**",        // 정적 리소스
                                "/fonts/**", "/js/**", "/css/**",  // 리소스
                                // API 전체 허용(하위 경로 포함)
                                "/api/**",
                                // WebSocket
                                "/ws/**",
                                // ===== 업무 화면 =====
                                "/db-list", "/sms-history", "/threshold-list", "/tablespaces", "/dailyChk", "/SysInfoDetail", "/TEST",
                                // ETL 관련
                                "/ETLJobList", "/etljob-history",
                                "/ETLjob-Scheduler"
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

/*
========= [원본 코드] =========

.requestMatchers(
        "/", "/index.html",
        "/favicon.ico", "/static/**", "/assets/**","/fonts/**",
        "/js/**", "/css/**","/db-list", "/sms-history", "/threshold-list", "/tablespaces", "/dailyChk",
        "/api/**","/ws/**","/api/alerts/**","/api/auth/**","/api/sysinfo/**","/api/SysInfoDetail/**","/api/sysinfo/by-date/**",
        "/api/sysinfo/log-summary/**","/SysInfoDetail","/api/pass/**",
        "/api/sysinfo/collected-dates-by-month/**","/TEST","/api/db-list/save/**",
        "/api/db-connection","/ETLJobList","/etljob-history",
        "/api/etl/job/**","/api/etl/run/**",
        "/api/etl/job/run/**","/api/etl/**"
 ).permitAll()

 - "/api/**"로 시작하면 하위 경로는 전부 허용됨.
 - 불필요하게 세부 경로 여러 번 추가할 필요 없음.
 - 예외적으로, "/ws/**", "/ETLJobList" 등만 별도 추가해주면 됨.
 */
