package com.rental.rental_system.controller;

import com.rental.rental_system.model.User;
import com.rental.rental_system.repository.UserRepository;
import com.rental.rental_system.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    private final UserRepository userRepo;
    private final UserService service;

    public AuthController(UserRepository userRepo, UserService service) {
        this.userRepo = userRepo;
        this.service = service;
    }

    // show register page
    @GetMapping("/register")
    public String showRegister(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    // save user
    @PostMapping("/register")
    public String register(@ModelAttribute User user, Model model) {
        service.register(user);
        model.addAttribute("success", "Account created successfully! Please login.");
        return "login";
    }

    // show login page
    @GetMapping("/login")
    public String showLogin() {
        return "login";
    }

    // login logic
    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {

        // ADMIN LOGIN
        if (email.equals("admin@gmail.com") && password.equals("admin123")) {
            session.setAttribute("admin", "admin");
            return "redirect:/admin";
        }

        // USER LOGIN
        User user = service.login(email, password);

        if (user != null) {
            session.setAttribute("user", user);
            return "redirect:/properties";
        } else {
            model.addAttribute("error", "Invalid credentials");
            return "login";
        }
    }

    // logout
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "login";
    }

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {

        Object user = session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", user);

        return "profile";
    }

    @GetMapping("/edit-profile")
    public String editProfile(HttpSession session, Model model) {
        model.addAttribute("user", session.getAttribute("user"));
        return "edit-profile";
    }

    @PostMapping("/update-profile")
    public String updateProfile(@ModelAttribute User user, HttpSession session) {

        User existingUser = (User) session.getAttribute("user");

        user.setId(existingUser.getId()); // VERY IMPORTANT

        userRepo.save(user);

        session.setAttribute("user", user); // update session

        return "redirect:/profile";
    }
}