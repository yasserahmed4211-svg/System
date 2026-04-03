package com.example.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
@Profile("dev") // تعمل فقط على dev
public class SwaggerSecurityDevConfig {

  @Bean
  @Order(0) // يسبق السلسلة العامة
  public SecurityFilterChain swaggerChainDev(HttpSecurity http) throws Exception {
    return http
      .securityMatcher("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/api-docs/**")
      .csrf(csrf -> csrf.disable())
      .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()) // عام بدون صلاحيات
      .build();
  }
}
