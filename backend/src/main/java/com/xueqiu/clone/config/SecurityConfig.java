package com.xueqiu.clone.config;

import com.xueqiu.clone.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置。
 *
 * 鉴权策略：
 * - 所有 GET 读取接口（信息流、个股、评论、搜索、行情、指数、用户主页）对未登录用户开放，
 *   方便浏览与演示。
 * - 写操作（发帖、点赞、评论）需要携带有效 JWT（Bearer Token），否则返回 401。
 * - 使用无状态会话（STATELESS），每次请求通过 JWT 还原登录态。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(org.springframework.security.config.Customizer.withDefaults())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/ws/**").permitAll()   // WebSocket 握手与 SockJS 端点公开
                .requestMatchers("/api/auth/**").permitAll()
                // 读取类接口全部公开
                .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                // 写操作需鉴权
                .requestMatchers(HttpMethod.POST, "/api/posts").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/feed/*/like").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/posts/*/comments").authenticated()
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // H2 控制台使用 iframe，需关闭 X-Frame-Options
            .headers(h -> h.frameOptions(fo -> fo.disable()));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
