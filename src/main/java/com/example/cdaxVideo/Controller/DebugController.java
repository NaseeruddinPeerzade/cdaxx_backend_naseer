// DebugController.java
package com.example.cdaxVideo.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;


@RestController
@RequestMapping("/api/test")
public class DebugController {
    
    // Test 1: Exact same pattern but in different controller
    @GetMapping("/modules/{moduleId}/assessments")
    public ResponseEntity<String> testEndpoint(@PathVariable Long moduleId) {
        return ResponseEntity.ok("Test endpoint works! Module: " + moduleId);
    }
    
    // Test 2: In the same controller but different method
    @GetMapping("/{moduleId}/test")
    public ResponseEntity<String> testModule(@PathVariable Long moduleId) {
        return ResponseEntity.ok("Module test works: " + moduleId);
    }
}
