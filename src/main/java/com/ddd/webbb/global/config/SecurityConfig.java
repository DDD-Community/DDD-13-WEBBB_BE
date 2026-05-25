package com.ddd.webbb.global.config;

import com.ddd.webbb.global.auth.CustomOAuth2UserService;
import com.ddd.webbb.global.auth.JwtAuthFilter;
import com.ddd.webbb.global.auth.JwtAuthenticationEntryPoint;
import com.ddd.webbb.global.auth.OAuth2LoginFailureHandler;
import com.ddd.webbb.global.auth.OAuth2LoginSuccessHandler;
import com.ddd.webbb.global.auth.RedisOAuth2AuthorizationRequestRepository;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    private final RedisOAuth2AuthorizationRequestRepository authorizationRequestRepository;
    private final Optional<ClientRegistrationRepository> clientRegistrationRepository;

    public SecurityConfig(
            JwtAuthFilter jwtAuthFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            CustomOAuth2UserService customOAuth2UserService,
            OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
            OAuth2LoginFailureHandler oAuth2LoginFailureHandler,
            RedisOAuth2AuthorizationRequestRepository authorizationRequestRepository,
            Optional<ClientRegistrationRepository> clientRegistrationRepository) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.oAuth2LoginFailureHandler = oAuth2LoginFailureHandler;
        this.authorizationRequestRepository = authorizationRequestRepository;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(
                        exception ->
                                exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .authorizeHttpRequests(
                        auth ->
                                auth
                                        // Swagger
                                        .requestMatchers(
                                                "/swagger-ui/**",
                                                "/v3/api-docs/**",
                                                "/swagger-ui.html")
                                        .permitAll()
                                        // Actuator (health, info만 공개)
                                        .requestMatchers("/actuator/health", "/actuator/info")
                                        .permitAll()
                                        // OAuth2 로그인 진입점
                                        .requestMatchers(
                                                "/oauth2/authorization/**", "/login/oauth2/code/**")
                                        .permitAll()
                                        // Auth 공개 엔드포인트
                                        .requestMatchers(
                                                "/api/auth/signup/email",
                                                "/api/auth/login/email",
                                                "/api/auth/oauth/**",
                                                "/api/auth/refresh")
                                        .permitAll()
                                        // /api/users/me 는 인증 필요 (wildcard 보다 먼저 선언)
                                        .requestMatchers("/api/users/me")
                                        .authenticated()
                                        // 읽기 전용 API
                                        .requestMatchers(HttpMethod.GET, "/api/users/**")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.GET, "/api/posts/**")
                                        .permitAll()
                                        // 나머지는 인증 필요
                                        .anyRequest()
                                        .authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        if (clientRegistrationRepository.isPresent()) {
            http.oauth2Login(
                    oauth2 ->
                            oauth2.authorizationEndpoint(
                                            endpoint ->
                                                    endpoint.authorizationRequestRepository(
                                                            authorizationRequestRepository))
                                    .userInfoEndpoint(
                                            userInfo ->
                                                    userInfo.userService(customOAuth2UserService))
                                    .successHandler(oAuth2LoginSuccessHandler)
                                    .failureHandler(oAuth2LoginFailureHandler));
        }

        return http.build();
    }
}
