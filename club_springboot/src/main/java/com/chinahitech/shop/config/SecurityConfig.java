package com.chinahitech.shop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .cors()
                .configurationSource(corsConfigurationSource())
                .and()
                .csrf().disable()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .antMatchers(
                        "/student/login",
                        "/student/register",
                        "/student/getEmail",
                        "/student/getValidate",
                        "/student/validateEmail",
                        "/manager/login",
                        "/manager/register",
                        "/manager/getEmail",
                        "/manager/getValidate",
                        "/manager/validateEmail",
                        "/topManager/login",
                        "/topManager/getEmail",
                        "/topManager/getValidate",
                        "/topManager/validateEmail",
                        "/upload/*.jpg",
                        "/upload/*.jpeg",
                        "/upload/*.png",
                        "/upload/*.gif",
                        "/upload/**/*.jpg",
                        "/upload/**/*.jpeg",
                        "/upload/**/*.png",
                        "/upload/**/*.gif"
                ).permitAll()
                .antMatchers("/upload/**").hasAnyRole("STUDENT", "MANAGER", "TOP_MANAGER")
                .antMatchers(
                        "/activity/all",
                        "/activity/studentDetail",
                        "/activity/getVideo",
                        "/activity/top",
                        "/group/all",
                        "/group/studentDetail",
                        "/group/getVideo",
                        "/group/top",
                        "/weather/**",
                        "/videotest.mp4"
                ).hasAnyRole("STUDENT", "MANAGER", "TOP_MANAGER")
                .antMatchers(
                        "/activity/applyJoin",
                        "/activity/myJoinedActivities",
                        "/individualActivity/allActivities",
                        "/individualActivity/joinActivity",
                        "/individualGroup/allGroups",
                        "/individualGroup/applyJoinGroup"
                ).hasAnyRole("STUDENT", "TOP_MANAGER")
                .antMatchers(
                        "/activity/addGroup",
                        "/activity/managerDetail",
                        "/activity/modifyDescription",
                        "/activity/uploadZip",
                        "/activity/submitZip",
                        "/activity/uploadPhoto",
                        "/activity/submitPhoto",
                        "/activity/getAttachment",
                        "/activity/getActivityApplicants",
                        "/activity/auditApply",
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
                .antMatchers(
                        "/activity/allApps",
                        "/activity/appDetail",
                        "/activity/accept",
                        "/activity/reject",
                        "/group/allApps",
                        "/group/appDetail",
                        "/group/accept",
                        "/group/reject",
                        "/individualActivity/getActivityMembers",
                        "/individualGroup/getAllStudents",
                        "/individualGroup/getGroupMembers"
                ).hasRole("TOP_MANAGER")
                .antMatchers("/student/**").hasAnyRole("STUDENT", "TOP_MANAGER")
                .antMatchers("/manager/**").hasAnyRole("MANAGER", "TOP_MANAGER")
                .antMatchers("/topManager/**").hasRole("TOP_MANAGER")
                .anyRequest().denyAll()
                .and()
                .exceptionHandling()
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"success\":false,\"code\":50008,\"message\":\"未认证或登录已过期\"}");
                })
                .and()
                .addFilterBefore(new JwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
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
            return Arrays.asList(
                    "http://localhost:9527",
                    "http://localhost:9528",
                    "http://localhost:9529",
                    "http://localhost:8080"
            );
        }
        return Arrays.asList(configured.split("\\s*,\\s*"));
    }
}
