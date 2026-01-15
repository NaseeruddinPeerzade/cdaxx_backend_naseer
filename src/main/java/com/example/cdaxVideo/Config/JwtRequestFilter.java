package com.example.cdaxVideo.Config;

import com.example.cdaxVideo.Service.CustomUserDetailsService;

import io.jsonwebtoken.ExpiredJwtException;

import com.example.cdaxVideo.Config.JwtTokenUtil;
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
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // Skip filtering for OPTIONS requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        
        // Skip filtering for multipart/form-data requests (for now)
        String contentType = request.getContentType();
        if (contentType != null && contentType.startsWith("multipart/form-data")) {
            System.out.println("⚠️ Skipping JWT filter for multipart request: " + request.getRequestURI());
            return true;
        }
        
        return false;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain chain)
            throws ServletException, IOException {
        
        System.out.println("🔍 JWT Filter processing: " + request.getRequestURI());
        System.out.println("🔍 Content-Type: " + request.getContentType());
        System.out.println("🔍 Method: " + request.getMethod());
        
        final String requestTokenHeader = request.getHeader("Authorization");
           
        String username = null;
        String jwtToken = null;
        
        // JWT Token is in the form "Bearer token". Remove Bearer word and get only the Token
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            try {
                username = jwtTokenUtil.getUsernameFromToken(jwtToken);
            } catch (IllegalArgumentException e) {
                System.out.println("   ❌ Unable to get JWT Token: " + e.getMessage());
            } catch (ExpiredJwtException e) {
                System.out.println("   ⚠️ JWT Token has expired");
            } catch (Exception e) {
                System.out.println("   ❌ Error parsing token: " + e.getClass().getName() + " - " + e.getMessage());
            }
        } else {
            System.out.println("   ⚠️ JWT Token does not begin with Bearer String or no Authorization header");
            if (requestTokenHeader != null) {
                System.out.println("   ⚠️ Actual header starts with: '" + 
                    (requestTokenHeader.length() > 7 ? requestTokenHeader.substring(0, 7) : requestTokenHeader) + "'");
            }
        }
        
        // Once we get the token validate it.
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            System.out.println("   🔐 Attempting to load user: " + username);
            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                
                // if token is valid configure Spring Security to manually set authentication
                if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {
                    
                    System.out.println("   ✅ Token validated for user: " + username);
                    
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // After setting the Authentication in the context, we specify
                    // that the current user is authenticated. So it passes the Spring Security Configurations successfully.
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    System.out.println("   ✅ Authentication set in SecurityContext");
                } else {
                    System.out.println("   ❌ Token validation failed for user: " + username);
                }
            } catch (UsernameNotFoundException e) {
                System.out.println("   ❌ User not found: " + username);
            }
        } else {
            if (username == null) {
                System.out.println("   ⚠️ Username is null, skipping authentication");
            } else {
                System.out.println("   ⚠️ Authentication already exists in context");
            }
        }
        
        System.out.println("   ➡️ Proceeding with filter chain...");
        chain.doFilter(request, response);
    }
}