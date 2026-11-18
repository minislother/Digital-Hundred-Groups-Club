package com.chinahitech.shop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    /**
     * 配置 Spring Security 的登录、权限和跨域策略。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/knowledge/**").hasAnyRole("STUDENT", "MANAGER", "TOP_MANAGER")
                .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**",
                        "/actuator/prometheus").hasRole("TOP_MANAGER")
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers(
                        "/student/login",
                        "/student/register",
                        "/student/getEmail",
                        "/student/getValidate",
                        "/student/validateEmail",
                        "/manager/login",
                        "/manager/getEmail",
                        "/manager/getValidate",
                        "/manager/validateEmail",
                        "/topManager/login",
                        "/topManager/getEmail",
                        "/topManager/getValidate",
                        "/topManager/validateEmail"








                ).permitAll()
                .requestMatchers(request -> request.getRequestURI().matches("/upload/.*\\.(jpg|jpeg|png|gif)" )).permitAll()
                .requestMatchers("/upload/**").hasAnyRole("STUDENT", "MANAGER", "TOP_MANAGER")
                .requestMatchers(
                        "/activity/all",
                        "/activity/search",
                        "/activity/searchSlice",
                        "/activity/studentDetail",
                        "/activity/getVideo",
                        "/activity/top",
                        "/group/all",
                        "/group/studentDetail",
                        "/group/getVideo",
                        "/group/top",
                        "/getWeather",
                        "/weather/**",
                        "/videotest.mp4"
                ).hasAnyRole("STUDENT", "MANAGER", "TOP_MANAGER")
                .requestMatchers(
                        "/activity/applyJoin",
                        "/activity/myJoinedActivities",
                        "/application/myApps",
                        "/application/submit",
                        "/application/updateAttachment",
                        "/application/uploadZip",
                        "/individualActivity/allActivities",
                        "/individualActivity/joinActivity",
                        "/individualGroup/allGroups",
                        "/individualGroup/applyJoinGroup"
                ).hasAnyRole("STUDENT", "TOP_MANAGER")
                .requestMatchers(
                        "/activity/addGroup",
                        "/activity/addActivity",
                        "/activity/managerDetail",
                        "/activity/myActivity",
                        "/activity/myApps",
                        "/activity/modifyDescription",
                        "/activity/modifyInfo",
                        "/activity/uploadZip",
                        "/activity/submitZip",
                        "/activity/uploadPhoto",
                        "/activity/submitPhoto",
                        "/activity/getAttachment",
                        "/activity/getActivityApplicants",
                        "/activity/auditApply",
                        "/application/recApps",
                        "/application/recApp",
                        "/application/accept",
                        "/application/reject",
                        "/group/addGroup",
                        "/group/managerDetail",
                        "/group/modifydescription",
                        "/group/uploadzip",
                        "/group/submitzip",
                        "/group/uploadphoto",
                        "/group/submitphoto",
                        "/group/getattachment",
                        "/individualActivity/getAllStudents",
                        "/individualActivity/getAllApplyStudents",
                        "/individualActivity/addActivityStudent",
                        "/individualActivity/modifyGroupStudent",
                        "/individualActivity/deleteActivityStudent",
                        "/individualActivity/allManagedActivities",
                        "/individualActivity/accept",
                        "/individualActivity/reject",
                        "/individualGroup/getStudentsByGroup",
                        "/individualGroup/getGroupApplyList",
                        "/individualGroup/acceptJoin",
                        "/individualGroup/rejectJoin",
                        "/individualGroup/addGroupStudent",
                        "/individualGroup/modifyGroupStudent",
                        "/individualGroup/deleteGroupStudent",
                        "/individualGroup/allManagedGroups",
                        "/individualGroup/transferStatus",
                        "/individualGroup/updatePermission"
                ).hasAnyRole("MANAGER", "TOP_MANAGER")
                .requestMatchers(
                        "/activity/allApps",
                        "/search/index/rebuildActivities",
                        "/search/index/status",
                        "/activity/appDetail",
                        "/activity/accept",
                        "/activity/reject",
                        "/application/all",
                        "/manager/register",
                        "/group/allApps",
                        "/group/appDetail",
                        "/group/accept",
                        "/group/reject",
                        "/individualActivity/getActivityMembers",
                        "/individualGroup/getAllStudents",
                        "/individualGroup/getGroupMembers"
                ).hasRole("TOP_MANAGER")
                .requestMatchers("/student/**").hasAnyRole("STUDENT", "TOP_MANAGER")
                .requestMatchers("/manager/**").hasAnyRole("MANAGER", "TOP_MANAGER")
                .requestMatchers("/topManager/**").hasRole("TOP_MANAGER")
                .anyRequest().denyAll())
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"success\":false,\"code\":50008,\"message\":\"未认证或登录已过期\"}");
                }))
                .addFilterBefore(new JwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins());
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Token", "X-User-Id"));
        config.setExposedHeaders(Arrays.asList("Content-Disposition"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private List<String> allowedOrigins() {
        String configured = System.getenv("CORS_ALLOWED_ORIGINS");
        if (configured == null || configured.trim().isEmpty()) {
            configured = System.getProperty("cors.allowed-origins");
        }
        if (configured == null || configured.trim().isEmpty()) {
            // 本地开发时浏览器可能使用 localhost 或 127.0.0.1 打开前端；两者对浏览器来说是不同源。
            return Arrays.asList(
                    "http://localhost:9527",
                    "http://localhost:9533",
                    "http://localhost:9531",
                    "http://localhost:9532",
                    "http://localhost:8080",
                    "http://127.0.0.1:9527",
                    "http://127.0.0.1:9533",
                    "http://127.0.0.1:9531",
                    "http://127.0.0.1:9532",
                    "http://127.0.0.1:8080"
            );
        }
        return Arrays.asList(configured.split("\\s*,\\s*"));
    }
}
