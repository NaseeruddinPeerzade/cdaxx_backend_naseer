package com.example.cdaxVideo.DTO;

import java.util.ArrayList;
import java.util.List;

public class CartSummaryDTO {
    private List<CartItemDTO> items = new ArrayList<>();
    private Integer itemCount;
    private Double totalPrice; // Original total price
    private Double discountedPrice; // Price after all discounts
    private Double discountAmount; // Total discount amount
    
    // Constructors
    public CartSummaryDTO() {}
    
    public CartSummaryDTO(List<CartItemDTO> items, Integer itemCount, Double totalPrice, 
                         Double discountedPrice, Double discountAmount) {
        this.items = items;
        this.itemCount = itemCount;
        this.totalPrice = totalPrice;
        this.discountedPrice = discountedPrice;
        this.discountAmount = discountAmount;
    }
    
    // Getters and Setters
    public List<CartItemDTO> getItems() { return items; }
    public void setItems(List<CartItemDTO> items) { this.items = items; }
    
    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
    
    public Double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Double totalPrice) { this.totalPrice = totalPrice; }
    
    public Double getDiscountedPrice() { return discountedPrice; }
    public void setDiscountedPrice(Double discountedPrice) { this.discountedPrice = discountedPrice; }
    
    public Double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(Double discountAmount) { this.discountAmount = discountAmount; }
    
    // Helper method to calculate bulk discount
    public void applyBulkDiscount() {
        if (totalPrice == null || discountedPrice == null) return;
        
        // Apply additional 10% discount for 2+ items
        if (itemCount != null && itemCount >= 2) {
            double bulkDiscounted = discountedPrice * 0.9;
            discountAmount = totalPrice - bulkDiscounted;
            discountedPrice = bulkDiscounted;
        }
    }
}