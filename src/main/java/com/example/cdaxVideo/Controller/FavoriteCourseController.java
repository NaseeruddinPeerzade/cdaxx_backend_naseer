package com.example.cdaxVideo.Controller;

import com.example.cdaxVideo.Service.FavoriteCourseService;
import com.example.cdaxVideo.DTO.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteCourseController {
    private final FavoriteCourseService favoriteService;
    
    public FavoriteCourseController(FavoriteCourseService favoriteService) {
        this.favoriteService = favoriteService;
    }
    
    @GetMapping("/{userId}")
    public ResponseEntity<List<FavoriteDTO>> getUserFavorites(@PathVariable Long userId) {
        List<FavoriteDTO> favorites = favoriteService.getUserFavorites(userId);
        return ResponseEntity.ok(favorites);
    }
    
    @PostMapping("/{userId}/add/{courseId}")
    public ResponseEntity<FavoriteDTO> addToFavorites(
            @PathVariable Long userId,
            @PathVariable Long courseId) {
        FavoriteDTO favorite = favoriteService.addToFavorites(userId, courseId);
        return ResponseEntity.ok(favorite);
    }
    
    @DeleteMapping("/{userId}/remove/{courseId}")
    public ResponseEntity<Void> removeFromFavorites(
            @PathVariable Long userId,
            @PathVariable Long courseId) {
        favoriteService.removeFromFavorites(userId, courseId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{userId}/check/{courseId}")
    public ResponseEntity<Boolean> isFavorite(
            @PathVariable Long userId,
            @PathVariable Long courseId) {
        boolean isFavorite = favoriteService.isCourseFavorite(userId, courseId);
        return ResponseEntity.ok(isFavorite);
    }
}