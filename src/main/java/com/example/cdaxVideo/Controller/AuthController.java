package com.example.cdaxVideo.Controller;

import com.example.cdaxVideo.Entity.User;
import com.example.cdaxVideo.Service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    // ---------------------- REGISTER ----------------------
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


    // ---------------------- LOGIN ----------------------
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

//    // -------- Toggle Subscription ----------
//    @PostMapping("/toggleSubscription")
//    public ResponseEntity<Map<String, Object>> toggleSubscription(@RequestParam String email) {
//        boolean updated = authService.toggleSubscription(email);
//        Map<String, Object> response = new HashMap<>();
//
//        if (updated) {
//            response.put("status", "success");
//            response.put("message", "Subscription status updated");
//        } else {
//            response.put("status", "error");
//            response.put("message", "User not found");
//        }
//
//        return ResponseEntity.ok(response);
//    }
}