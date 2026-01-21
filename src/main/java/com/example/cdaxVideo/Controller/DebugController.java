// DebugController.java
package com.example.cdaxVideo.Controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/api/debug")
public class DebugController {
    
    @GetMapping("/security-status")
    public Map<String, Object> securityStatus(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", new Date());
        response.put("requestPath", request.getServletPath());
        response.put("authentication", auth != null ? auth.getName() : "null");
        response.put("isAuthenticated", auth != null && auth.isAuthenticated());
        response.put("authorities", auth != null ? auth.getAuthorities().toString() : "null");
        
        // Headers
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        response.put("headers", headers);
        
        return response;
    }
    
    @GetMapping("/test-public")
    public Map<String, String> testPublic() {
        return Map.of(
            "message", "This is a public endpoint",
            "timestamp", new Date().toString(),
            "status", "OK"
        );
    }
    
    @GetMapping("/test-protected")
    public Map<String, String> testProtected() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Map.of(
            "message", "This is a protected endpoint",
            "user", auth != null ? auth.getName() : "anonymous",
            "timestamp", new Date().toString(),
            "status", "OK"
        );
    }
}