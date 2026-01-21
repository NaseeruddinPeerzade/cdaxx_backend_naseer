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
import java.util.Arrays;
import java.util.List;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    // List of public endpoints that don't need JWT validation
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        // Auth endpoints
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
        "/api/auth/getUserByEmail",
        
        // Public resources
        "/uploads/",
        
        // Public GET endpoints
        "/api/courses",
        "/api/courses/",
        "/api/courses/public/",
        
        // Debug endpoints
        "/api/debug/",
        
        // THIS IS THE CRITICAL LINE - module assessments should be public
        "/api/modules/",
        "/api/videos/",
        
        // Swagger
        "/swagger-ui/",
        "/v3/api-docs/",
        "/swagger-ui.html"
    );
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();
        
        System.out.println("\n🔍 JWT Filter Check - shouldNotFilter:");
        System.out.println("   Path: " + path);
        System.out.println("   Method: " + method);
        
        // Skip OPTIONS requests (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            System.out.println("   ✅ Skipping: OPTIONS request");
            return true;
        }
        
        // Check if path matches any public endpoint
        for (String publicPath : PUBLIC_PATHS) {
            if (path.startsWith(publicPath) && 
                (publicPath.endsWith("/") || path.equals(publicPath) || path.startsWith(publicPath + "/"))) {
                System.out.println("   ✅ Skipping: Public endpoint (" + publicPath + ")");
                return true;
            }
        }
        
        // Special case: GET requests to module assessments should be public
        if ("GET".equalsIgnoreCase(method) && path.matches("/api/modules/\\d+/assessments")) {
            System.out.println("   ✅ Skipping: GET /api/modules/{id}/assessments (public)");
            return true;
        }
        
        // Special case: GET requests to courses by ID should be public
        if ("GET".equalsIgnoreCase(method) && path.matches("/api/courses/\\d+")) {
            System.out.println("   ✅ Skipping: GET /api/courses/{id} (public)");
            return true;
        }
        
        System.out.println("   ➡️ Will filter: Requires authentication");
        return false;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain chain)
            throws ServletException, IOException {
        
        // Log request details
        String requestPath = request.getRequestURI();
        String method = request.getMethod();
        
        System.out.println("\n=== JWT Filter Processing ===");
        System.out.println("Path: " + requestPath);
        System.out.println("Method: " + method);
        
        // Since shouldNotFilter already skipped public endpoints, 
        // we know this endpoint requires authentication
        
        final String requestTokenHeader = request.getHeader("Authorization");
        
        if (requestTokenHeader == null || !requestTokenHeader.startsWith("Bearer ")) {
            System.out.println("❌ Missing or invalid Authorization header");
            System.out.println("Header value: " + requestTokenHeader);
            
            // For GET requests to certain endpoints, we might want to allow without auth
            // but since shouldNotFilter didn't skip, we should require auth
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, 
                "Unauthorized: No JWT token found or invalid format");
            return;
        }
        
        String jwtToken = requestTokenHeader.substring(7);
        String username = null;
        
        try {
            username = jwtTokenUtil.getUsernameFromToken(jwtToken);
            System.out.println("Extracted Username: " + username);
        } catch (IllegalArgumentException e) {
            System.out.println("❌ Unable to get JWT Token: " + e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, 
                "Unauthorized: Unable to get JWT token");
            return;
        } catch (ExpiredJwtException e) {
            System.out.println("⚠️ JWT Token has expired");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, 
                "Unauthorized: JWT Token has expired");
            return;
        } catch (Exception e) {
            System.out.println("❌ Error parsing token: " + e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, 
                "Unauthorized: Invalid JWT token");
            return;
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
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, 
                        "Unauthorized: Invalid JWT token");
                    return;
                }
            } catch (UsernameNotFoundException e) {
                System.out.println("❌ User not found: " + username);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, 
                    "Unauthorized: User not found");
                return;
            } catch (Exception e) {
                System.out.println("❌ Error loading user: " + e.getMessage());
                e.printStackTrace();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, 
                    "Unauthorized: Error processing authentication");
                return;
            }
        }
        
        System.out.println("➡️ Continuing filter chain...");
        chain.doFilter(request, response);
    }
}