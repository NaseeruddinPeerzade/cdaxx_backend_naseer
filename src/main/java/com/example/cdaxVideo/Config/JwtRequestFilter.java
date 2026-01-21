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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
        
        System.out.println("🔍 JWT Filter Check: " + method + " " + path);
        
        // Skip OPTIONS (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            System.out.println("   ✅ Skip: OPTIONS request");
            return true;
        }
        
        // Skip auth endpoints
        if (path.startsWith("/api/auth/")) {
            System.out.println("   ✅ Skip: Auth endpoint");
            return true;
        }
        
        // Skip uploads
        if (path.startsWith("/uploads/")) {
            System.out.println("   ✅ Skip: Public uploads");
            return true;
        }
        
        // Skip Swagger
        if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")) {
            System.out.println("   ✅ Skip: Swagger");
            return true;
        }
        
        // Skip debug
        if (path.startsWith("/api/debug/")) {
            System.out.println("   ✅ Skip: Debug endpoint");
            return true;
        }
        
        // 🔥 ONLY SKIP THESE SPECIFIC PUBLIC ENDPOINTS (GET only)
        if ("GET".equalsIgnoreCase(method)) {
            // Course list and single course
            if (path.equals("/api/courses") || path.matches("^/api/courses/\\d+$")) {
                System.out.println("   ✅ Skip: Public course browsing");
                return true;
            }
            
            // Module details (NOT assessments!)
            if (path.matches("^/api/modules/\\d+$")) {
                System.out.println("   ✅ Skip: Public module details");
                return true;
            }
            
            // Video details (NOT progress!)
            if (path.matches("^/api/videos/\\d+$")) {
                System.out.println("   ✅ Skip: Public video details");
                return true;
            }
        }
        
        // 🔒 Everything else (including /api/modules/{id}/assessments) requires JWT
        System.out.println("   🔒 Validate: JWT required");
        return false;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain chain)
            throws ServletException, IOException {
        
        String path = request.getServletPath();
        System.out.println("\n🔐 JWT Validation: " + path);
        
        final String requestTokenHeader = request.getHeader("Authorization");
        String username = null;
        String jwtToken = null;
        
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            System.out.println("   🔑 Token found (length: " + jwtToken.length() + ")");
            
            try {
                username = jwtTokenUtil.getUsernameFromToken(jwtToken);
                System.out.println("   👤 Username: " + username);
            } catch (IllegalArgumentException e) {
                System.out.println("   ❌ Invalid JWT Token");
            } catch (ExpiredJwtException e) {
                System.out.println("   ⚠️ JWT Token expired");
            } catch (Exception e) {
                System.out.println("   ❌ JWT Error: " + e.getMessage());
            }
        } else {
            System.out.println("   ⚠️ No Authorization header or invalid format");
        }
        
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                
                if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {
                    System.out.println("   ✅ Token valid - Authentication set");
                    
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                        );
                    
                    authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    System.out.println("   ❌ Token validation failed");
                }
            } catch (UsernameNotFoundException e) {
                System.out.println("   ❌ User not found: " + username);
            }
        } else if (username == null) {
            System.out.println("   ❌ No username extracted - will return 401");
        }
        
        chain.doFilter(request, response);
    }
}
