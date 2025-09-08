package com.securevoting.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import com.securevoting.model.User;
import com.securevoting.model.UserRole;
import com.securevoting.repository.UserRepository;

/**
 * Home controller for basic navigation and landing pages.
 * Handles role-based dashboard routing.
 */
@Controller
public class HomeController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        
        if (userId != null) {
            // Get user role and redirect to appropriate dashboard
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getRole() == UserRole.ADMIN) {
                return "redirect:/admin/dashboard";
            }
        }
        
        // Default to voter dashboard
        return "dashboard";
    }
}