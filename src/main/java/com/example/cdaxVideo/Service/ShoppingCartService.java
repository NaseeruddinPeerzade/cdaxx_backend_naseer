package com.example.cdaxVideo.Service;

import org.springframework.stereotype.Service;
import com.example.cdaxVideo.Entity.Course;
import com.example.cdaxVideo.Entity.ShoppingCartItem;
import com.example.cdaxVideo.Entity.User;
import com.example.cdaxVideo.Entity.UserCoursePurchase;
import com.example.cdaxVideo.Repository.ShoppingCartRepository;
import com.example.cdaxVideo.Repository.CourseRepository;
import com.example.cdaxVideo.Repository.UserRepository;
import com.example.cdaxVideo.Repository.UserCoursePurchaseRepository;
import com.example.cdaxVideo.DTO.*;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ShoppingCartService {
    private final ShoppingCartRepository cartRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final UserCoursePurchaseRepository purchaseRepository;
    
    public ShoppingCartService(ShoppingCartRepository cartRepository,
                              CourseRepository courseRepository,
                              UserRepository userRepository,
                              UserCoursePurchaseRepository purchaseRepository) {
        this.cartRepository = cartRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.purchaseRepository = purchaseRepository;
    }
    
    // ========== CRITICAL FIXES ==========
    
    // ✅ FIXED: Use optimized method with JOIN FETCH
    public List<CartItemDTO> getCartItems(Long userId) {
        // Use the optimized method that fetches course eagerly
        return cartRepository.findByUserIdWithCourse(userId).stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }
    
    // ✅ FIXED: Use optimized method for single item
    public CartItemDTO addToCart(Long userId, Long courseId) {
        // Check if already in cart
        if (cartRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new RuntimeException("Course already in cart");
        }
        
        // Check if user already purchased this course
        boolean alreadyPurchased = purchaseRepository.existsByUserIdAndCourseId(userId, courseId);
        if (alreadyPurchased) {
            throw new RuntimeException("You already own this course");
        }
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Course not found"));
        
        ShoppingCartItem cartItem = new ShoppingCartItem();
        cartItem.setUser(user);
        cartItem.setCourse(course);
        cartItem.setaddedAt(LocalDateTime.now());
        
        ShoppingCartItem saved = cartRepository.save(cartItem);
        
        // Return DTO using optimized fetch
        return cartRepository.findByUserIdAndCourseIdWithDetails(userId, courseId)
            .map(this::mapToDTO)
            .orElseThrow(() -> new RuntimeException("Failed to retrieve cart item after saving"));
    }
    
    // ✅ FIXED: Safe DTO mapping with null checks AND discount handling
    private CartItemDTO mapToDTO(ShoppingCartItem item) {
        CartItemDTO dto = new CartItemDTO();
        dto.setId(item.getId());
        
        // Course is guaranteed to be loaded because we used JOIN FETCH
        Course course = item.getCourse();
        if (course != null) {
            dto.setCourseId(course.getId());
            dto.setCourseTitle(course.getTitle());
            dto.setThumbnailUrl(course.getThumbnailUrl());
            
            // Handle nullable prices
            Double originalPrice = course.getPrice();
            Double discountedPrice = course.getDiscountPrice();
            Integer duration = course.getTotalDuration();
            
            // Set basic fields
            dto.setDuration(duration != null ? duration : 0);
            
            // Calculate and set discount fields
            if (discountedPrice != null && originalPrice != null && 
                originalPrice > 0 && discountedPrice < originalPrice) {
                // Course has discount
                dto.setHasDiscount(true);
                dto.setOriginalPrice(originalPrice);
                dto.setDiscountedPrice(discountedPrice);
                dto.setPrice(discountedPrice); // Current price is discounted price
                dto.setDiscountPercentage(((originalPrice - discountedPrice) / originalPrice) * 100);
            } else {
                // No discount or invalid prices
                dto.setHasDiscount(false);
                dto.setOriginalPrice(originalPrice != null ? originalPrice : 0.0);
                dto.setDiscountedPrice(originalPrice != null ? originalPrice : 0.0);
                dto.setPrice(originalPrice != null ? originalPrice : 0.0);
                dto.setDiscountPercentage(0.0);
            }
        } else {
            // Fallback values if course is null (shouldn't happen with JOIN FETCH)
            dto.setHasDiscount(false);
            dto.setOriginalPrice(0.0);
            dto.setDiscountedPrice(0.0);
            dto.setPrice(0.0);
            dto.setDiscountPercentage(0.0);
        }
        
        dto.setAddedAt(item.getaddedAt());
        return dto;
    }
    
    // ========== EXISTING METHODS (Updated) ==========
    
    public void removeFromCart(Long userId, Long courseId) {
        cartRepository.deleteByUserIdAndCourseId(userId, courseId);
    }
    
    public void clearCart(Long userId) {
        cartRepository.deleteAllByUserId(userId);
    }
    
    public boolean isCourseInCart(Long userId, Long courseId) {
        return cartRepository.existsByUserIdAndCourseId(userId, courseId);
    }
    
    public CartSummaryDTO getCartSummary(Long userId) {
        List<CartItemDTO> items = getCartItems(userId); // Uses optimized method
        
        if (items.isEmpty()) {
            CartSummaryDTO emptySummary = new CartSummaryDTO();
            emptySummary.setItems(new ArrayList<>());
            emptySummary.setItemCount(0);
            emptySummary.setTotalPrice(0.0);
            emptySummary.setDiscountedPrice(0.0);
            emptySummary.setDiscountAmount(0.0);
            return emptySummary;
        }
        
        // Calculate original total (sum of original prices)
        double totalOriginal = items.stream()
            .mapToDouble(item -> item.getOriginalPrice() != null ? item.getOriginalPrice() : 0.0)
            .sum();
        
        // Calculate discounted total (sum of discounted/actual prices)
        double totalDiscounted = items.stream()
            .mapToDouble(item -> item.getDiscountedPrice() != null ? item.getDiscountedPrice() : 0.0)
            .sum();
        
        // Apply bulk discount for 2+ items (additional 10% off)
        double finalDiscountedPrice = totalDiscounted;
        if (items.size() >= 2) {
            finalDiscountedPrice = totalDiscounted * 0.9; // Additional 10% bulk discount
        }
        
        CartSummaryDTO summary = new CartSummaryDTO();
        summary.setItems(items);
        summary.setItemCount(items.size());
        summary.setTotalPrice(totalOriginal);
        summary.setDiscountedPrice(finalDiscountedPrice);
        summary.setDiscountAmount(totalOriginal - finalDiscountedPrice);
        
        return summary;
    }
    
    public CheckoutResponseDTO checkout(Long userId, CheckoutRequestDTO request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<CartItemDTO> cartItems = getCartItems(userId); // Uses optimized method
        List<String> purchasedCourseTitles = new ArrayList<>();
        double totalAmount = 0.0;
        
        // Validate cart items
        if (cartItems.isEmpty()) {
            return CheckoutResponseDTO.failure("Your cart is empty");
        }
        
        // Process each course in cart
        for (CartItemDTO cartItem : cartItems) {
            // Check if course still exists
            Course course = courseRepository.findById(cartItem.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found: " + cartItem.getCourseId()));
            
            // Check if already purchased (in case of race condition)
            boolean alreadyPurchased = purchaseRepository.existsByUserIdAndCourseId(userId, cartItem.getCourseId());
            if (alreadyPurchased) {
                // Remove from cart but don't purchase again
                cartRepository.deleteByUserIdAndCourseId(userId, cartItem.getCourseId());
                continue;
            }
            
            // Create purchase record
            UserCoursePurchase purchase = new UserCoursePurchase();
            purchase.setUser(user);
            purchase.setCourse(course);
            purchase.setPurchasedOn(java.sql.Date.valueOf(LocalDateTime.now().toLocalDate()));
            
            purchaseRepository.save(purchase);
            purchasedCourseTitles.add(course.getTitle());
            
            // Use discounted price if available, otherwise use original price
            double itemPrice = cartItem.isHasDiscount() ? cartItem.getDiscountedPrice() : cartItem.getPrice();
            totalAmount += itemPrice;
            
            // Remove from cart after purchase
            cartRepository.deleteByUserIdAndCourseId(userId, cartItem.getCourseId());
        }
        
        if (purchasedCourseTitles.isEmpty()) {
            return CheckoutResponseDTO.failure("No courses were purchased");
        }
        
        // Generate order ID
        String orderId = "ORD-" + System.currentTimeMillis() + "-" + userId;
        
        // Apply coupon discount if provided
        double finalAmount = applyCoupon(totalAmount, request.getCouponCode());
        
        // Apply bulk discount for 2+ items (if not already applied in cart summary)
        if (cartItems.size() >= 2) {
            finalAmount = finalAmount * 0.9; // Additional 10% bulk discount
        }
        
        // Here you would integrate with payment gateway
        // For now, we'll assume payment is successful
        boolean paymentSuccess = processPayment(userId, finalAmount, request.getPaymentMethod());
        
        if (paymentSuccess) {
            return CheckoutResponseDTO.success(orderId, finalAmount, purchasedCourseTitles);
        } else {
            return CheckoutResponseDTO.failure("Payment failed. Please try again.");
        }
    }
    
    private double calculateDiscountedPrice(List<CartItemDTO> items) {
        if (items.isEmpty()) {
            return 0.0;
        }
        
        // Calculate total of discounted prices
        double totalDiscounted = items.stream()
            .mapToDouble(item -> item.getDiscountedPrice() != null ? item.getDiscountedPrice() : 0.0)
            .sum();
        
        // Apply bulk discount for 2+ items
        if (items.size() >= 2) {
            return totalDiscounted * 0.9; // 10% discount
        }
        
        return totalDiscounted;
    }
    
    private double applyCoupon(double amount, String couponCode) {
        if (couponCode == null || couponCode.isEmpty()) {
            return amount;
        }
        
        // Apply coupon logic here
        // Example: 20% off with coupon "SAVE20"
        if ("SAVE20".equalsIgnoreCase(couponCode)) {
            return amount * 0.8;
        }
        
        // Add more coupon codes as needed
        if ("WELCOME10".equalsIgnoreCase(couponCode)) {
            return amount * 0.9; // 10% discount
        }
        
        return amount;
    }
    
    private boolean processPayment(Long userId, double amount, String paymentMethod) {
        // TODO: Integrate with actual payment gateway
        // For now, simulate successful payment
        System.out.println("Processing payment for user " + userId + 
                          ": $" + String.format("%.2f", amount) + " via " + paymentMethod);
        
        // Simulate payment processing
        try {
            Thread.sleep(1000); // Simulate API call delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Return true for successful payment simulation
        // In real implementation, this would call payment gateway API
        return true;
    }
    
    // Helper method to get cart item count
    public int getCartItemCount(Long userId) {
        return (int) cartRepository.countByUserId(userId);
    }
    
    // Helper method to check if cart is empty
    public boolean isCartEmpty(Long userId) {
        return cartRepository.countByUserId(userId) == 0;
    }
}