package com.example.cdaxVideo.Config;

import com.example.cdaxVideo.Service.CustomUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();
        
        System.out.println("🔍 Checking if should filter: " + method + " " + path);
        
        // ✅ Skip ALL public endpoints (not just OPTIONS)
        if (isPublicEndpoint(path, method)) {
            System.out.println("✅ Skipping JWT filter for public endpoint");
            return true;
        }
        
        return false;
    }
    
    private boolean isPublicEndpoint(String path, String method) {
        // Always skip OPTIONS (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
        
        // List ALL public endpoints from your SecurityConfig
        return path.startsWith("/api/auth/") ||
               path.startsWith("/api/public/") ||
               path.startsWith("/api/debug/") ||
               path.startsWith("/api/courses/public") ||
               (path.equals("/api/courses") && "GET".equalsIgnoreCase(method)) ||
               (path.matches("/api/courses/\\d+") && "GET".equalsIgnoreCase(method)) ||
               // FIXED: Add ALL module patterns
               (path.matches("/api/modules/\\d+") && "GET".equalsIgnoreCase(method)) ||
               (path.matches("/api/modules/\\d+/videos") && "GET".equalsIgnoreCase(method)) ||
               (path.matches("/api/modules/\\d+/assessments") && "GET".equalsIgnoreCase(method)) ||
               (path.matches("/api/modules/course/\\d+") && "GET".equalsIgnoreCase(method)) ||
               // Better: Use wildcard pattern
               (path.matches("/api/modules/\\d+/.*") && "GET".equalsIgnoreCase(method)) ||
               path.startsWith("/api/course/assessment/") ||
               path.startsWith("/api/assessments") ||
               path.startsWith("/uploads/") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs/") ||
               path.equals("/actuator/health") ||
               path.equals("/actuator/info") ||
               path.startsWith("/api/dashboard/public") ||
               path.startsWith("/api/videos/public") ||
               path.startsWith("/api/test/");
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain chain)
            throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        System.out.println("🔐 JWT Filter processing: " + requestPath);
        
        final String requestTokenHeader = request.getHeader("Authorization");
        
        String username = null;
        String jwtToken = null;
        
        // Extract JWT token from Authorization header
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            
            try {
                username = jwtTokenUtil.getUsernameFromToken(jwtToken);
                System.out.println("Extracted Username: " + username);
            } catch (Exception e) {
                System.out.println("Error extracting username: " + e.getMessage());
            }
        }
        
        // Validate token and set authentication
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                
                if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {
                    System.out.println("✅ Token validated for: " + username);
                    
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails, 
                            null, 
                            userDetails.getAuthorities()
                        );
                    
                    authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                System.out.println("Error setting authentication: " + e.getMessage());
            }
        }
        
        chain.doFilter(request, response);
    }
}