package com.example.cdaxVideo.Controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/debug")
public class DebugController {
    
    @GetMapping("/test")
    public ResponseEntity<?> test() {
        return ResponseEntity.ok(Map.of("message", "Debug endpoint is working"));
    }
    
    @PostMapping("/headers")
    public ResponseEntity<?> debugHeaders(HttpServletRequest request) {
        Map<String, Object> debugInfo = new HashMap<>();
        
        // Collect all headers
        Map<String, String> headers = new HashMap<>();
        request.getHeaderNames().asIterator()
            .forEachRemaining(headerName -> 
                headers.put(headerName, request.getHeader(headerName)));
        
        debugInfo.put("headers", headers);
        debugInfo.put("method", request.getMethod());
        debugInfo.put("path", request.getRequestURI());
        debugInfo.put("contentType", request.getContentType());
        debugInfo.put("authorization", request.getHeader("Authorization"));
        debugInfo.put("origin", request.getHeader("Origin"));
        debugInfo.put("serverPort", request.getServerPort());
        debugInfo.put("remoteAddr", request.getRemoteAddr());
        debugInfo.put("queryString", request.getQueryString());
        
        return ResponseEntity.ok(debugInfo);
    }
    
    @GetMapping("/headers-get")
    public ResponseEntity<?> debugHeadersGet(HttpServletRequest request) {
        return debugHeaders(request);
    }
    
    @PostMapping("/multipart")
    public ResponseEntity<?> testMultipart(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "data", required = false) String data,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("fileReceived", file != null);
        response.put("fileName", file != null ? file.getOriginalFilename() : null);
        response.put("fileSize", file != null ? file.getSize() : 0);
        response.put("data", data);
        response.put("authHeaderPresent", authHeader != null);
        response.put("authHeaderValid", authHeader != null && authHeader.startsWith("Bearer "));
        response.put("authHeaderPrefix", authHeader != null ? 
            authHeader.substring(0, Math.min(authHeader.length(), 20)) : null);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/security-info")
    public ResponseEntity<?> getSecurityInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", new Date());
        response.put("authenticated", auth != null && auth.isAuthenticated());
        
        if (auth != null) {
            response.put("principal", auth.getPrincipal());
            response.put("name", auth.getName());
            response.put("authorities", auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        }
        
        // Test endpoint accessibility
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("/api/course/assessment/questions", "GET - Should be PUBLIC");
        endpoints.put("/api/course/assessment/submit", "POST - Should require AUTH");
        endpoints.put("/api/modules/{id}/assessments", "GET - Should be PUBLIC");
        endpoints.put("/api/course/assessment/status", "GET - Should be PUBLIC");
        endpoints.put("/api/course/assessment/can-attempt", "GET - Should be PUBLIC");
        
        response.put("assessmentEndpoints", endpoints);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/test-jwt")
    public ResponseEntity<?> testJwt(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> response = new HashMap<>();
        
        response.put("authHeaderPresent", authHeader != null);
        
        if (authHeader != null) {
            response.put("authHeaderStartsWithBearer", authHeader.startsWith("Bearer "));
            
            if (authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                response.put("tokenLength", token.length());
                response.put("tokenPrefix", token.substring(0, Math.min(10, token.length())));
            }
        }
        
        // Get authentication info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        response.put("authenticated", auth != null && auth.isAuthenticated());
        response.put("principal", auth != null ? auth.getPrincipal().getClass().getSimpleName() : null);
        response.put("principalDetails", auth != null ? auth.getPrincipal().toString() : null);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/test-jwt-get")
    public ResponseEntity<?> testJwtGet(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        return testJwt(authHeader);
    }
    
    // Echo endpoint - returns whatever you send
    @PostMapping("/echo")
    public ResponseEntity<?> echo(@RequestBody(required = false) Map<String, Object> body,
                                  @RequestHeader Map<String, String> headers) {
        Map<String, Object> response = new HashMap<>();
        response.put("body", body);
        response.put("headers", headers);
        response.put("receivedAt", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
    
    // Test CORS specifically
    @GetMapping("/cors-test")
    public ResponseEntity<?> corsTest(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("origin", request.getHeader("Origin"));
        response.put("accessControlAllowOrigin", request.getHeader("Access-Control-Allow-Origin"));
        response.put("method", request.getMethod());
        response.put("corsHeaders", Map.of(
            "Origin", request.getHeader("Origin"),
            "Access-Control-Request-Method", request.getHeader("Access-Control-Request-Method"),
            "Access-Control-Request-Headers", request.getHeader("Access-Control-Request-Headers")
        ));
        return ResponseEntity.ok(response);
    }
    
    // Test specific assessment endpoint
    @PostMapping("/test-assessment-auth")
    public ResponseEntity<?> testAssessmentAuth(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("endpoint", "/api/debug/test-assessment-auth");
        response.put("method", request.getMethod());
        response.put("contentType", request.getContentType());
        response.put("authorizationHeader", authHeader);
        response.put("hasValidAuthHeader", 
            authHeader != null && authHeader.startsWith("Bearer "));
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        response.put("userAuthenticated", 
            auth != null && auth.isAuthenticated());
        response.put("authenticationName", auth != null ? auth.getName() : null);
        
        return ResponseEntity.ok(response);
    }
}