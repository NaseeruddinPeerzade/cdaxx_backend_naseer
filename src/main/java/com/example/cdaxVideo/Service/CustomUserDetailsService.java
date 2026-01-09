package com.example.cdaxVideo.Service;

import com.example.cdaxVideo.Entity.User;
import com.example.cdaxVideo.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        return new org.springframework.security.core.userdetails.User(
            user.getEmail(),
            user.getPassword(),
            getAuthorities(user)
        );
    }
    
    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        // If user has no role, default to USER
        String role = (user.getRole() != null && !user.getRole().isEmpty()) 
            ? user.getRole() 
            : "USER";
        
        // Ensure role has ROLE_ prefix for Spring Security
        if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }
        
        return Collections.singletonList(new SimpleGrantedAuthority(role));
    }
}