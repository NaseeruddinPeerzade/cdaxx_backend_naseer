package com.example.cdaxVideo.Config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
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
    
    @Autowired
    public SecurityConfig(JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint, 
                         JwtRequestFilter jwtRequestFilter) {
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtRequestFilter = jwtRequestFilter;
        System.out.println("✅✅✅ SECURITY CONFIG CONSTRUCTOR CALLED!");
        System.out.println("   jwtRequestFilter is null: " + (jwtRequestFilter == null));
    }
    
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        System.out.println("✅✅✅ SECURITY FILTER CHAIN BEAN CREATED!");
        
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .sessionManagement(management -> management
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // ALLOW ALL OPTIONS REQUESTS (CORS preflight)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // PUBLIC ENDPOINTS
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/debug/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                
                // PUBLIC GET ENDPOINTS (specific patterns only)
                .requestMatchers(HttpMethod.GET, "/api/courses").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/courses/{id}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/modules/{id:\\d+}").permitAll()  // Only numeric IDs
                .requestMatchers(HttpMethod.GET, "/api/videos/{id:\\d+}").permitAll()   // Only numeric IDs
                
                // EVERYTHING ELSE REQUIRES AUTHENTICATION
                .anyRequest().authenticated()
            );
        
        // Add JWT filter
        System.out.println("🔗 Adding JWT filter before UsernamePasswordAuthenticationFilter");
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow all origins for debugging
        configuration.setAllowedOriginPatterns(List.of("*"));
        
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));
        
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}