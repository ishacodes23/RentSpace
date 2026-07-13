package com.rental.rental_system.service;

import com.rental.rental_system.model.User;
import com.rental.rental_system.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository repo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public void register(User user) {
        user.setPassword(encoder.encode(user.getPassword())); // encrypt password
        repo.save(user);
    }

    public User login(String email, String password) {
        User user = repo.findByEmail(email);

        if (user != null && encoder.matches(password, user.getPassword())) {
            return user;
        }
        return null;
    }
}