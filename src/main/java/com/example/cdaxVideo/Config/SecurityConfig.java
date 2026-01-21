package com.example.cdaxVideo.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtRequestFilter jwtRequestFilter;
    
    public SecurityConfig(JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint, 
                         JwtRequestFilter jwtRequestFilter) {
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtRequestFilter = jwtRequestFilter;
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .sessionManagement(management -> management
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // =============== PUBLIC ENDPOINTS ===============
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // Debug
                .requestMatchers("/api/debug/**").permitAll()
                
                // Auth endpoints
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/register",
                    "/api/auth/jwt/login",
                    "/api/auth/jwt/register",
                    "/api/auth/jwt/validate",
                    "/api/auth/jwt/refresh",
                    "/api/auth/forgot-password",
                    "/api/auth/reset-password",
                    "/api/auth/verify-email",
                    "/api/auth/firstName",
                    "/api/auth/getUserByEmail"
                ).permitAll()
                
                // Public files
                .requestMatchers("/uploads/**").permitAll()
                
                // Public course browsing (GET only) - SPECIFIC ENDPOINTS ONLY
                .requestMatchers(HttpMethod.GET, "/api/courses").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/courses/{id}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/modules/{id}").permitAll() // ONLY module details
                .requestMatchers(HttpMethod.GET, "/api/videos/{id}").permitAll()  // ONLY video details
                
                // Swagger
                .requestMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/webjars/**",
                    "/swagger-resources/**"
                ).permitAll()
                
                // Actuator
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                
                // =============== PROTECTED ENDPOINTS ===============
                // 🔒 ALL ASSESSMENT ENDPOINTS REQUIRE AUTH
                .requestMatchers("/api/assessments/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/modules/{id}/assessments").authenticated()
                .requestMatchers("/api/course/assessment/**").authenticated()
                .requestMatchers("/api/questions/**").authenticated()
                
                // User profile
                .requestMatchers("/api/auth/profile/**").authenticated()
                .requestMatchers("/api/auth/jwt/me").authenticated()
                .requestMatchers("/api/auth/change-password").authenticated()
                
                // Course management
                .requestMatchers("/api/courses/subscribed/**").authenticated()
                .requestMatchers("/api/courses/user/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/courses").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/courses/{id}").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/courses/{id}").authenticated()
                .requestMatchers("/api/courses/{id}/enroll").authenticated()

                // Streak
                .requestMatchers("/api/streak/**").authenticated()
                .requestMatchers("/api/profile/streak").authenticated()
                
                // Video progress
                .requestMatchers(HttpMethod.POST, "/api/videos/{id}/progress").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/videos/{id}/complete").authenticated()
                
                // User data
                .requestMatchers("/api/favorites/**").authenticated()
                .requestMatchers("/api/dashboard/**").authenticated()
                .requestMatchers("/api/cart/**").authenticated()
                .requestMatchers("/api/users/**").authenticated()
                
                // Purchase
                .requestMatchers(HttpMethod.POST, "/api/purchase").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/modules/*/unlock-*").authenticated()
                
                // Default - everything else requires auth
                .anyRequest().authenticated()
            );
        
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "http://192.168.*:*",
            "https://*.up.railway.app",
            "https://*.railway.app",
            "https://*.vercel.app",
            "https://*.onrender.com",
            "https://*",
            "http://*"
        ));

        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));
        
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", "Content-Type", "Accept", "Origin",
            "X-Requested-With", "Access-Control-Request-Method",
            "Access-Control-Request-Headers", "X-CSRF-Token",
            "Cache-Control", "Pragma", "x-auth-token"
        ));
        
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization", "Content-Disposition",
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials", "x-auth-token"
        ));
        
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}