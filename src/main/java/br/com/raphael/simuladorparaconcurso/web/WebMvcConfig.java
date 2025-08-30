package br.com.raphael.simuladorparaconcurso.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ProfAuthInterceptor profAuthInterceptor; // injetado

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(profAuthInterceptor)
                .addPathPatterns("/prof/**")
                .excludePathPatterns("/prof/login", "/css/**", "/js/**", "/assets/**", "/images/**", "/webjars/**", "/favicon.ico");
    }
}
