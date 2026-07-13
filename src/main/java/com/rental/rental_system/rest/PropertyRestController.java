package com.rental.rental_system.rest;

import com.rental.rental_system.model.Property;
import com.rental.rental_system.repository.PropertyRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PropertyRestController {

    private final PropertyRepository repo;

    public PropertyRestController(PropertyRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/properties")
    public List<Property> getAll() {
        return repo.findAll();
    }

    @PostMapping("/properties")
    public Property add(@RequestBody Property p) {
        return repo.save(p);
    }
}