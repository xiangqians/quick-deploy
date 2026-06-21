package org.xiangqian.quick.deploy.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author xiangqian
 * @date 2026/01/19 11:12
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig implements WebMvcConfigurer {

    // 处理静态资源
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
    }

    // 密码加密器
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 安全过滤链
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // 允许未经授权访问
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/error", "/proj/*/deploy/webhook", "/static/**").permitAll()
                        .anyRequest().authenticated())
                // 忽略 webhook 的 CSRF
                .csrf(csrf -> csrf.ignoringRequestMatchers("/proj/*/deploy/webhook"))
                // 登录配置
                .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/", true).permitAll())
                // 在认证之前注册一个过滤器
                .addFilterBefore(new UsernamePasswordAuthenticationBeforeFilter(), UsernamePasswordAuthenticationFilter.class)
                // 处理 CSRF 异常，重定向到登录页
                .exceptionHandling(exception -> exception.accessDeniedHandler((request, response, accessDeniedException) -> {
                    if ("POST".equals(request.getMethod()) && "/login".equals(request.getServletPath())) {
                        String ctxPath = request.getContextPath();
                        response.sendRedirect(ctxPath + "/login");
                    } else {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    }
                }))
                .build();
    }

}
