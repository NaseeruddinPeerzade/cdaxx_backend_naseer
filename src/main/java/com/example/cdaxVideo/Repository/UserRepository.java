package com.example.cdaxVideo.Repository;

import com.example.cdaxVideo.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

 
    
@Query("SELECT u FROM User u WHERE LOWER(TRIM(u.email)) = LOWER(TRIM(:email))")
Optional<User> findByEmail(@Param("email") String email);


    // ✅ Find user by phone number (NOT mobile)
    Optional<User> findByPhoneNumber(String phoneNumber);

    // ✅ Check if email exists
    boolean existsByEmail(String email);

    // ✅ Check if phone number exists (NOT mobile)
    boolean existsByPhoneNumber(String phoneNumber);

    Optional<User> findByRefreshToken(String refreshToken);
}