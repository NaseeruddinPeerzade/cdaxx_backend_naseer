package com.example.cdaxVideo.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
        // http
        //     // Enable CORS with custom configuration
        //     .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
        //     // Disable CSRF for JWT (REST API)
        //     .csrf(csrf -> csrf.disable())
            
        //     // Configure exception handling
        //     .exceptionHandling(handling -> handling
        //         .authenticationEntryPoint(jwtAuthenticationEntryPoint))
            
        //     // Make session stateless
        //     .sessionManagement(management -> management
        //         .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
        //     // Configure authorization
        //     .authorizeHttpRequests(authz -> authz
        //         // =============== PUBLIC ENDPOINTS ===============
        //         // Allow all OPTIONS requests (CORS preflight)
        //         .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
        //         // Debug endpoints
        //         .requestMatchers("/api/debug/**").permitAll()
        //         .requestMatchers("/api/public/**").permitAll()
        //         // Authentication endpoints
        //         .requestMatchers(
        //             "/api/auth/login",
        //             "/api/auth/register",
        //             "/api/auth/jwt/login",
        //             "/api/auth/jwt/register",
        //             "/api/auth/jwt/validate",
        //             "/api/auth/jwt/refresh",
        //             "/api/auth/forgot-password",
        //             "/api/auth/reset-password",
        //             "/api/auth/verify-email",
        //             "/api/auth/firstName",
        //             "/api/auth/getUserByEmail"
        //         ).permitAll()
                
        //         // Public file access
        //         .requestMatchers("/uploads/**").permitAll()
                
        //         // Public course endpoints
        //         .requestMatchers("/api/courses/public/**").permitAll()
        //         .requestMatchers(HttpMethod.GET, "/api/courses").permitAll()
        //         .requestMatchers(HttpMethod.GET, "/api/courses/{id}").permitAll()
                
        //         // Public assessment endpoints - REORDERED FOR PRIORITY
        //         // CRITICAL FIX: Move modules endpoint FIRST before any similar patterns
        //         .requestMatchers(HttpMethod.GET, "/api/modules/{moduleId}/assessments").permitAll()
        //         .requestMatchers(HttpMethod.GET, "/api/course/assessment/**").permitAll()
        //         .requestMatchers(HttpMethod.GET, "/api/assessments/**").permitAll()
        //         .requestMatchers(HttpMethod.GET, "/api/course/assessment/status").permitAll()
        //         .requestMatchers(HttpMethod.GET, "/api/course/assessment/can-attempt").permitAll()
        //         .requestMatchers(HttpMethod.GET, "/api/course/assessment/questions").permitAll()

        //         // Assessment submissions require authentication
        //         .requestMatchers(HttpMethod.GET, "/api/test/**").permitAll()
        //         .requestMatchers(HttpMethod.POST, "/api/modules/{moduleId}/assessments").authenticated()
        //         .requestMatchers(HttpMethod.POST, "/api/course/assessment/**").authenticated()
        //         .requestMatchers(HttpMethod.POST, "/api/assessments/**").authenticated()
        //         .requestMatchers(HttpMethod.POST, "/api/course/assessment/submit").authenticated()
                
        //         // Public video endpoints
        //         .requestMatchers("/api/videos/public/**").permitAll()
                
        //         // Legacy public endpoints
        //         .requestMatchers("/api/dashboard/public").permitAll()
                
        //         // Swagger/OpenAPI
        //         .requestMatchers(
        //             "/swagger-ui/**",
        //             "/v3/api-docs/**",
        //             "/swagger-ui.html",
        //             "/webjars/**",
        //             "/swagger-resources/**"
        //         ).permitAll()
                
        //         // Actuator endpoints
        //         .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                
        //         // =============== PROTECTED ENDPOINTS ===============
        //         // User profile endpoints
        //         .requestMatchers("/api/auth/profile/**").authenticated()
        //         .requestMatchers("/api/auth/jwt/me").authenticated()
        //         .requestMatchers("/api/auth/change-password").authenticated()
                
        //         // Course management - NOTE: Keep BELOW the specific modules endpoint
        //         .requestMatchers("/api/courses/subscribed/**").authenticated()
        //         .requestMatchers("/api/courses/user/**").authenticated()
        //         .requestMatchers(HttpMethod.POST, "/api/courses").authenticated()
        //         .requestMatchers(HttpMethod.PUT, "/api/courses/{id}").authenticated()
        //         .requestMatchers(HttpMethod.DELETE, "/api/courses/{id}").authenticated()
        //         // CRITICAL: This line was causing the conflict - it comes AFTER modules rule now
        //         .requestMatchers("/api/courses/{id}/enroll").authenticated()

        //         // Streak endpoints (ALL require authentication)
        //         .requestMatchers(HttpMethod.GET, "/api/streak/**").authenticated()
        //         .requestMatchers(HttpMethod.GET, "/api/profile/streak").authenticated()
                
        //         // Video progress
        //         .requestMatchers(HttpMethod.POST, "/api/videos/{id}/progress").authenticated()
        //         .requestMatchers(HttpMethod.POST, "/api/videos/{id}/complete").authenticated()
                
        //         // User data
        //         .requestMatchers("/api/favorites/**").authenticated()
        //         .requestMatchers("/api/dashboard/**").authenticated()
        //         .requestMatchers("/api/cart/**").authenticated()
        //         .requestMatchers("/api/users/**").authenticated()
                
        //         // Default - all other requests require authentication
        //         .anyRequest().authenticated()
        //     );
        
        // // Add JWT filter before Spring Security's authentication filter
        // http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow Render and Vercel domains with wildcards
        configuration.setAllowedOriginPatterns(List.of(
            // Local development
            "http://localhost:*",
            "http://127.0.0.1:*",
            "http://192.168.*:*",
            
            // Render domains
            "https://*.onrender.com",
            
            // Vercel domains
            "https://*.vercel.app",
            
            // Specific domains for safety
            "https://cdax-app.onrender.com",
            "https://cdax-app.vercel.app"
        ));

        // Allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));
        
        // Allowed headers
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "X-CSRF-Token",
            "Cache-Control",
            "Pragma"
        ));
        
        // Exposed headers (visible to browser)
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Disposition",
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials"
        ));
        
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Cache preflight response for 1 hour
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