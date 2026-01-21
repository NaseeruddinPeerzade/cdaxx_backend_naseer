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
import java.util.Collections;

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
        
        System.out.println("\n🎯 JWT Filter - shouldNotFilter()");
        System.out.println("📍 Path: " + path);
        System.out.println("📍 Method: " + method);
        
        // Skip OPTIONS (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            System.out.println("✅ SKIP: OPTIONS (CORS Preflight)");
            return true;
        }
        
        // Skip auth endpoints
        if (path.startsWith("/api/auth/")) {
            System.out.println("✅ SKIP: Auth endpoint");
            return true;
        }
        
        // Skip uploads
        if (path.startsWith("/uploads/")) {
            System.out.println("✅ SKIP: Public uploads");
            return true;
        }
        
        // Skip Swagger
        if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")) {
            System.out.println("✅ SKIP: Swagger");
            return true;
        }
        
        // Skip debug
        if (path.startsWith("/api/debug/")) {
            System.out.println("✅ SKIP: Debug endpoint");
            return true;
        }
        
        // Skip actuator
        if (path.startsWith("/actuator/")) {
            System.out.println("✅ SKIP: Actuator");
            return true;
        }
        
        // 🔥 ONLY SKIP THESE SPECIFIC PUBLIC ENDPOINTS (GET only)
        if ("GET".equalsIgnoreCase(method)) {
            // Course list
            if (path.equals("/api/courses")) {
                System.out.println("✅ SKIP: GET /api/courses (PUBLIC)");
                return true;
            }
            
            // Single course
            if (path.matches("^/api/courses/\\d+$")) {
                System.out.println("✅ SKIP: GET /api/courses/{id} (PUBLIC)");
                return true;
            }
            
            // Module details ONLY (NOT assessments!)
            if (path.matches("^/api/modules/\\d+$")) {
                System.out.println("✅ SKIP: GET /api/modules/{id} (PUBLIC - module details only)");
                return true;
            }
            
            // Video details ONLY (NOT progress!)
            if (path.matches("^/api/videos/\\d+$")) {
                System.out.println("✅ SKIP: GET /api/videos/{id} (PUBLIC - video details only)");
                return true;
            }
        }
        
        // 🔒 Everything else requires JWT validation
        System.out.println("🔒 VALIDATE: JWT required for this endpoint");
        return false;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain chain)
            throws ServletException, IOException {
        
        String path = request.getServletPath();
        System.out.println("\n🔐 JWT FILTER - Processing: " + request.getMethod() + " " + path);
        
        // Log headers for debugging
        System.out.println("📋 Request Headers:");
        Collections.list(request.getHeaderNames()).forEach(headerName -> {
            if (headerName.equalsIgnoreCase("authorization") || 
                headerName.equalsIgnoreCase("content-type") ||
                headerName.equalsIgnoreCase("accept")) {
                System.out.println("   - " + headerName + ": " + request.getHeader(headerName));
            }
        });
        
        final String requestTokenHeader = request.getHeader("Authorization");
        String username = null;
        String jwtToken = null;
        
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            System.out.println("🔑 Token found (length: " + jwtToken.length() + ")");
            System.out.println("🔑 Token first 50 chars: " + jwtToken.substring(0, Math.min(50, jwtToken.length())) + "...");
            
            try {
                username = jwtTokenUtil.getUsernameFromToken(jwtToken);
                System.out.println("👤 Username extracted from token: " + username);
            } catch (IllegalArgumentException e) {
                System.out.println("❌ Invalid JWT Token format: " + e.getMessage());
            } catch (ExpiredJwtException e) {
                System.out.println("⚠️ JWT Token expired");
            } catch (Exception e) {
                System.out.println("❌ JWT parsing error: " + e.getClass().getName() + " - " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("⚠️ No Authorization header or doesn't start with 'Bearer '");
            if (requestTokenHeader != null) {
                System.out.println("   Header value: " + requestTokenHeader);
            }
        }
        
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                System.out.println("🔍 Loading user details for: " + username);
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                System.out.println("✅ User found: " + userDetails.getUsername());
                
                System.out.println("🔍 Validating token...");
                if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {
                    System.out.println("✅ Token valid");
                    
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                        );
                    
                    authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    System.out.println("✅ Authentication set in SecurityContext");
                    System.out.println("   Principal: " + authentication.getPrincipal());
                    System.out.println("   Authorities: " + authentication.getAuthorities());
                } else {
                    System.out.println("❌ Token validation failed");
                }
            } catch (UsernameNotFoundException e) {
                System.out.println("❌ User not found: " + username);
            } catch (Exception e) {
                System.out.println("❌ Error in user loading/validation: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            if (username == null) {
                System.out.println("❌ No username extracted from token - will return 401");
            } else {
                System.out.println("ℹ️ Authentication already exists in context");
            }
        }
        
        System.out.println("➡️ Continuing filter chain...");
        chain.doFilter(request, response);
    }
}