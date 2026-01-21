package com.example.cdaxVideo.Config;

import com.example.cdaxVideo.Service.CustomUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.annotation.PostConstruct;
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
    
    // Debug constructor
    public JwtRequestFilter() {
        System.out.println("✅✅✅ JWT REQUEST FILTER CONSTRUCTOR CALLED!");
    }
    
    @PostConstruct
    public void init() {
        System.out.println("✅✅✅ JWT FILTER INITIALIZED!");
        System.out.println("   jwtTokenUtil: " + (jwtTokenUtil != null ? "INJECTED" : "NULL"));
        System.out.println("   userDetailsService: " + (userDetailsService != null ? "INJECTED" : "NULL"));
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();
        
        System.out.println("\n🎯🎯🎯 JWT FILTER - shouldNotFilter() CALLED!");
        System.out.println("📍 Method: " + method);
        System.out.println("📍 Path: " + path);
        
        // Skip OPTIONS (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            System.out.println("✅ SKIP: OPTIONS request");
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
        
        // TEMPORARY: FORCE FILTER TO RUN FOR ALL REQUESTS (for debugging)
        System.out.println("🔒 FILTER WILL RUN FOR THIS REQUEST");
        return false;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain chain)
            throws ServletException, IOException {
        
        System.out.println("\n🔐🔐🔐 JWT FILTER - doFilterInternal() EXECUTING!");
        System.out.println("📍 Request: " + request.getMethod() + " " + request.getServletPath());
        
        // Log ALL headers
        System.out.println("📋 ALL REQUEST HEADERS:");
        Collections.list(request.getHeaderNames()).forEach(headerName -> {
            System.out.println("   " + headerName + ": " + request.getHeader(headerName));
        });
        
        final String requestTokenHeader = request.getHeader("Authorization");
        String username = null;
        String jwtToken = null;
        
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            System.out.println("🔑 Token extracted, length: " + jwtToken.length());
            
            try {
                username = jwtTokenUtil.getUsernameFromToken(jwtToken);
                System.out.println("👤 Username from token: " + username);
            } catch (Exception e) {
                System.out.println("❌ Error extracting username: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("⚠️ No valid Authorization header found");
            if (requestTokenHeader != null) {
                System.out.println("   Actual header value: " + requestTokenHeader);
            }
        }
        
        if (username != null) {
            System.out.println("🔍 Attempting to authenticate user: " + username);
            
            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                System.out.println("✅ UserDetails loaded successfully");
                
                if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {
                    System.out.println("✅ Token validated successfully");
                    
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                        );
                    
                    authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    System.out.println("✅ Authentication set in SecurityContext");
                } else {
                    System.out.println("❌ Token validation failed");
                }
            } catch (UsernameNotFoundException e) {
                System.out.println("❌ User not found: " + username);
            } catch (Exception e) {
                System.out.println("❌ Error during authentication: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("ℹ️ No username extracted, continuing without authentication");
        }
        
        System.out.println("➡️ Continuing to next filter in chain...");
        chain.doFilter(request, response);
        System.out.println("🏁 JWT Filter completed for: " + request.getServletPath());
    }
}