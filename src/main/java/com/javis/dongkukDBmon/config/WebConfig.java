package com.javis.dongkukDBmon.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // /, /reset-password, /db-list 등 1단계 경로
        registry.addViewController("/{spring:[^\\.]+}")
                .setViewName("forward:/index.html");
        // /reset-password/step2 등 2단계 이상 경로도 모두 index.html로!
        registry.addViewController("/**/{spring:[^\\.]+}")
                .setViewName("forward:/index.html");
    }

}