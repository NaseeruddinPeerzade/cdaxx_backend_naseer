package com.example.cdaxVideo.Controller;

import com.example.cdaxVideo.Service.ShoppingCartService;
import com.example.cdaxVideo.DTO.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class ShoppingCartController {
    private final ShoppingCartService cartService;
    
    public ShoppingCartController(ShoppingCartService cartService) {
        this.cartService = cartService;
    }
    
    @GetMapping("/{userId}")
    public ResponseEntity<List<CartItemDTO>> getCartItems(@PathVariable Long userId) {
        List<CartItemDTO> items = cartService.getCartItems(userId);
        return ResponseEntity.ok(items);
    }
    
    @GetMapping("/{userId}/summary")
    public ResponseEntity<CartSummaryDTO> getCartSummary(@PathVariable Long userId) {
        CartSummaryDTO summary = cartService.getCartSummary(userId);
        return ResponseEntity.ok(summary);
    }
    
    @PostMapping("/{userId}/add/{courseId}")
    public ResponseEntity<CartItemDTO> addToCart(
            @PathVariable Long userId,
            @PathVariable Long courseId) {
        CartItemDTO item = cartService.addToCart(userId, courseId);
        return ResponseEntity.ok(item);
    }
    
    @DeleteMapping("/{userId}/remove/{courseId}")
    public ResponseEntity<Void> removeFromCart(
            @PathVariable Long userId,
            @PathVariable Long courseId) {
        cartService.removeFromCart(userId, courseId);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/{userId}/clear")
    public ResponseEntity<Void> clearCart(@PathVariable Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{userId}/checkout")
    public ResponseEntity<CheckoutResponseDTO> checkout(
            @PathVariable Long userId,
            @RequestBody CheckoutRequestDTO request) {
        CheckoutResponseDTO response = cartService.checkout(userId, request);
        return ResponseEntity.ok(response);
    }
}