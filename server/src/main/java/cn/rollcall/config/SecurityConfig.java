package cn.rollcall.config;

import cn.rollcall.api.Problem;
import cn.rollcall.service.Sessions;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.cors.*;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, Sessions sessions, ObjectMapper json) throws Exception {
        var filter = new OncePerRequestFilter() {
            @Override
            protected boolean shouldNotFilter(HttpServletRequest r) {
                return "OPTIONS".equals(r.getMethod()) || !r.getRequestURI().startsWith("/api/v1/") || r.getRequestURI().equals("/api/v1/auth/login")
                        || ("POST".equals(r.getMethod()) && r.getRequestURI().equals("/api/v1/auth/register/teacher"))
                        || ("POST".equals(r.getMethod()) && r.getRequestURI().equals("/api/v1/auth/console-ticket/exchange"))
                        || ("POST".equals(r.getMethod()) && r.getRequestURI().equals("/api/v1/analytics/home-visit"))
                        || ("POST".equals(r.getMethod()) && r.getRequestURI().equals("/api/v1/feedback"))
                        || ("GET".equals(r.getMethod()) && r.getRequestURI().equals("/api/v1/site/about"));
            }

            @Override
            protected void doFilterInternal(HttpServletRequest r, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
                try {
                    String h = r.getHeader("Authorization");
                    var u = sessions.resolve(h != null && h.startsWith("Bearer ") ? h.substring(7) : null, r.getRequestURI());
                    var context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(new UsernamePasswordAuthenticationToken(u, null, List.of(new SimpleGrantedAuthority("ROLE_" + u.role))));
                    SecurityContextHolder.setContext(context);
                } catch (Exception e) {
                    int status = e instanceof Problem p ? p.status : 503;
                    response.setStatus(status);
                    response.setContentType("application/json;charset=UTF-8");
                    json.writeValue(response.getWriter(), Map.of("code", status, "message", e instanceof Problem ? e.getMessage() : "认证服务暂不可用"));
                    return;
                }
                try {
                    chain.doFilter(r, response);
                } finally {
                    SecurityContextHolder.clearContext();
                }
            }
        };
        return http.csrf(c -> c.disable()).cors(c -> {
                })
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.GET, "/api/v1/site/about").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/analytics/home-visit").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/feedback").permitAll()
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/register/teacher", "/api/v1/auth/console-ticket/exchange", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint((r, s, x) -> s.sendError(401)))
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class).build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors-origins}") String origins) {
        var c = new CorsConfiguration();
        c.setAllowedOrigins(Arrays.asList(origins.split(",")));
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Client"));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", c);
        return source;
    }
}
