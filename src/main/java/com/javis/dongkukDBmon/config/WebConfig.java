package com.javis.dongkukDBmon.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // SPA 라우팅을 위해 모든 경로를 index.html로 포워딩
        registry.addViewController("/{path:[^\\.]+}")
                .setViewName("forward:/index.html");
    }
}