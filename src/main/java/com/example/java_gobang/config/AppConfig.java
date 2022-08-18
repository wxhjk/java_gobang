package com.example.java_gobang.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class AppConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/**/**.css")
                .excludePathPatterns("/**/**.js")
                .excludePathPatterns("/**/*.jpg")
                .excludePathPatterns("/**/*.png")
                .excludePathPatterns("/login.html")
                .excludePathPatterns("/gobang/login")
                .excludePathPatterns("/register.html")
                .excludePathPatterns("/gobang/register");
    }
}
