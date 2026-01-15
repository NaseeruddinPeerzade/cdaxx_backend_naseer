package com.example.cdaxVideo.Controller;

import com.example.cdaxVideo.Entity.User;
import com.example.cdaxVideo.Repository.UserRepository;
import com.example.cdaxVideo.Service.AuthService;
import com.example.cdaxVideo.Config.JwtTokenUtil;
import com.example.cdaxVideo.DTO.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    // ==================== EXISTING ENDPOINTS (KEPT AS IS) ====================

    // ---------------------- REGISTER (Original) ----------------------
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerUser(@RequestBody User user) {
        String result = authService.registerUser(user);
        Map<String, Object> resp = new HashMap<>();

        switch (result) {
            case "Email already exists":
            case "Passwords do not match":
            case "Mobile number already registered":
                resp.put("success", false);
                resp.put("message", result);
                return ResponseEntity.badRequest().body(resp);

            case "Registration successful":
                resp.put("success", true);
                resp.put("message", result);
                return ResponseEntity.ok(resp);

            default:
                resp.put("success", false);
                resp.put("message", "Something went wrong");
                return ResponseEntity.status(500).body(resp);
        }
    }

    // ---------------------- LOGIN (Original) ----------------------
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> loginUser(@RequestBody User user) {
        String result = authService.loginUser(user);
        Map<String, Object> resp = new HashMap<>();

        switch (result) {
            case "Login successful":
                // GET FULL USER DETAILS
                User fullUser = authService.getUserByEmail(user.getEmail());

                resp.put("success", true);
                resp.put("message", "Login successful");
                resp.put("user", fullUser);   // ✅ IMPORTANT
                return ResponseEntity.ok(resp);

            case "Incorrect password":
                resp.put("success", false);
                resp.put("message", result);
                return ResponseEntity.status(401).body(resp);

            case "Email not found":
                resp.put("success", false);
                resp.put("message", result);
                return ResponseEntity.status(404).body(resp);

            default:
                resp.put("success", false);
                resp.put("message", "Something went wrong");
                return ResponseEntity.badRequest().body(resp);
        }
    }

    // ==================== NEW JWT ENDPOINTS ====================

    // ---------------------- REGISTER with JWT ----------------------
    @PostMapping("/jwt/register")
    public ResponseEntity<Map<String, Object>> registerUserWithJWT(@RequestBody User user) {
        try {
            Map<String, Object> result = authService.registerUserWithJWT(user);
            result.put("success", true);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        } catch (Exception e) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("message", "Registration failed");
            return ResponseEntity.status(500).body(resp);
        }
    }

    // ---------------------- LOGIN with JWT ----------------------
@PostMapping("/jwt/login")
public ResponseEntity<Map<String, Object>> loginUserWithJWT(@RequestBody Map<String, String> credentials) {
    try {
        String email = credentials.get("email");
        String password = credentials.get("password");

        if (email == null || password == null || email.isEmpty() || password.isEmpty()) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("message", "Email and password are required");
            return ResponseEntity.badRequest().body(resp);
        }

        Map<String, Object> result = authService.loginUserWithJWT(email, password);
        result.put("success", true);
        return ResponseEntity.ok(result);

    } catch (RuntimeException e) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", false);
        resp.put("message", e.getMessage());
        return ResponseEntity.status(401).body(resp);
    }
}


    // ---------------------- GET CURRENT USER (Protected) ----------------------
    @GetMapping("/jwt/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        try {
            UserDTO userDTO = authService.getCurrentUser();
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("user", userDTO);
            return ResponseEntity.ok(resp);
        } catch (RuntimeException e) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("message", e.getMessage());
            return ResponseEntity.status(401).body(resp);
        }
    }



    // ==================== PROFILE UPDATE ENDPOINTS ====================

@PutMapping("/profile/update")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<Map<String, Object>> updateProfile(@RequestBody Map<String, Object> updates) {
    try {
        UserDTO updatedUser = authService.updateProfile(updates);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Profile updated successfully");
        resp.put("user", updatedUser);
        return ResponseEntity.ok(resp);
    } catch (RuntimeException e) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", false);
        resp.put("message", e.getMessage());
        return ResponseEntity.badRequest().body(resp);
    }
}

// In AuthController - update /profile/me endpoint
@GetMapping("/profile/me")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<Map<String, Object>> getMyProfile() {
    try {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        // Use getFullUserProfile instead of getCurrentUser
        UserDTO userDTO = authService.getFullUserProfile(email);
        
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("user", userDTO);
        return ResponseEntity.ok(resp);
    } catch (RuntimeException e) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", false);
        resp.put("message", e.getMessage());
        return ResponseEntity.status(401).body(resp);
    }
}

private Map<String, Object> convertToProfileDTO(UserDTO userDTO) {
    Map<String, Object> profile = new HashMap<>();
    profile.put("id", userDTO.getId());
    profile.put("firstName", userDTO.getFirstName());
    profile.put("lastName", userDTO.getLastName());
    profile.put("email", userDTO.getEmail());
    profile.put("role", userDTO.getRole());
    profile.put("isNewUser", userDTO.getIsNewUser());
    
    // Get full user details from database for additional fields
    User user = authService.getUserById(userDTO.getId());
    if (user != null) {
        profile.put("phoneNumber", user.getPhoneNumber());
        profile.put("address", user.getAddress());
        profile.put("dateOfBirth", user.getDateOfBirth());
        profile.put("profileImage", user.getProfileImage());
    }
    
    return profile;
}

    // ---------------------- VALIDATE TOKEN ----------------------
    @PostMapping("/jwt/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestBody Map<String, String> tokenRequest) {
        try {
            String token = tokenRequest.get("token");
            if (token == null || token.isEmpty()) {
                throw new RuntimeException("Token is required");
            }
            
            boolean isValid = authService.validateToken(token);
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("valid", isValid);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("message", e.getMessage());
            resp.put("valid", false);
            return ResponseEntity.ok(resp);
        }
    }

    // ---------------------- REFRESH TOKEN ----------------------
@PostMapping("/jwt/refresh")
public ResponseEntity<Map<String, Object>> refreshToken(@RequestBody Map<String, String> body) {
    String refreshToken = body.get("refreshToken");

    User user = userRepository.findByRefreshToken(refreshToken)
            .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

    // Generate new access token
    String newAccessToken = jwtTokenUtil.generateToken(user.getEmail(), Map.of(
            "role", user.getRole(),
            "userId", user.getId()
    ));

    Map<String, Object> resp = new HashMap<>();
    resp.put("success", true);
    resp.put("accessToken", newAccessToken);
    return ResponseEntity.ok(resp);
}


    // ==================== EXISTING ENDPOINTS (CONTINUED) ====================

    @GetMapping("/test")
    public ResponseEntity<String> testServer() {
        return ResponseEntity.ok("Server is running!");
    }

    // -------- Get First Name ----------
    @GetMapping("/firstName")
    public Map<String, Object> getFirstName(@RequestParam String email) {
        String firstName = authService.getFirstNameByEmail(email);
        Map<String, Object> resp = new HashMap<>();

        if (firstName != null) {
            resp.put("status", "success");
            resp.put("firstName", firstName);
        } else {
            resp.put("status", "error");
            resp.put("message", "User not found");
        }
        return resp;
    }

    // -------- Get User by Email ----------
    @GetMapping("/getUserByEmail")
    public ResponseEntity<Map<String, Object>> getUserByEmail(@RequestParam String email) {
        User user = authService.getUserByEmail(email);
        Map<String, Object> response = new HashMap<>();

        if (user != null) {
            response.put("status", "success");
            response.put("email", user.getEmail());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            response.put("mobile", user.getPhoneNumber());
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "error");
            response.put("message", "User not found");
            return ResponseEntity.status(404).body(response);
        }
    }

    @PostMapping("/debug/hash")
public String generateHash(@RequestParam String password) {
    return new BCryptPasswordEncoder().encode(password);
}

    @PostMapping("/profile/upload-image")
    public ResponseEntity<Map<String, Object>> uploadProfileImage(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String authHeader) {
        
        System.out.println("📤 Profile image upload via AuthController");
        
        try {
            // Manual JWT validation
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Authentication required"
                ));
            }
            
            String token = authHeader.substring(7);
            String email = jwtTokenUtil.getUsernameFromToken(token);
            
            if (email == null || !jwtTokenUtil.validateToken(token)) {
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Invalid token"
                ));
            }
            
            User user = authService.getUserByEmail(email);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "User not found"
                ));
            }
            
            Map<String, Object> result = authService.uploadProfileImage(file, user.getId());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Upload failed: " + e.getMessage()
            ));
        }
    }

}