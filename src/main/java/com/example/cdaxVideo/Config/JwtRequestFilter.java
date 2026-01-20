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
        // ✅ FIXED: Only skip OPTIONS requests for CORS preflight
        // DO NOT skip multipart requests - they need authentication too!
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain chain)
            throws ServletException, IOException {
        
        // Log request details for debugging
        String requestPath = request.getRequestURI();
        String method = request.getMethod();
        String contentType = request.getContentType();
        
        System.out.println("\n=== JWT Filter Debug ===");
        System.out.println("Path: " + requestPath);
        System.out.println("Method: " + method);
        System.out.println("Content-Type: " + contentType);
        
        final String requestTokenHeader = request.getHeader("Authorization");
        System.out.println("Authorization Header Present: " + (requestTokenHeader != null));
        
        String username = null;
        String jwtToken = null;
        
        // Extract JWT token from Authorization header
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            System.out.println("JWT Token Length: " + jwtToken.length());
            
            try {
                username = jwtTokenUtil.getUsernameFromToken(jwtToken);
                System.out.println("Extracted Username: " + username);
            } catch (IllegalArgumentException e) {
                System.out.println("❌ Unable to get JWT Token: " + e.getMessage());
            } catch (ExpiredJwtException e) {
                System.out.println("⚠️ JWT Token has expired");
            } catch (Exception e) {
                System.out.println("❌ Error parsing token: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        } else {
            if (requestTokenHeader != null) {
                System.out.println("⚠️ Invalid Authorization format. Starts with: '" + 
                    requestTokenHeader.substring(0, Math.min(requestTokenHeader.length(), 10)) + "'");
            } else {
                System.out.println("⚠️ No Authorization header present");
            }
        }
        
        // Validate token and set authentication
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            System.out.println("🔐 Loading user details for: " + username);
            
            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                
                if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {
                    System.out.println("✅ Token validated successfully");
                    
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
                    System.out.println("✅ Authentication set in SecurityContext");
                } else {
                    System.out.println("❌ Token validation failed");
                }
            } catch (UsernameNotFoundException e) {
                System.out.println("❌ User not found: " + username);
            } catch (Exception e) {
                System.out.println("❌ Error loading user: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            if (username == null) {
                System.out.println("ℹ️ No username extracted - proceeding without authentication");
            } else {
                System.out.println("ℹ️ Authentication already exists in context");
            }
        }
        
        System.out.println("➡️ Continuing filter chain...");
        chain.doFilter(request, response);
    }
}