package com.rental.rental_system.controller;

import com.rental.rental_system.model.Property;
import com.rental.rental_system.model.User;
import com.rental.rental_system.model.Booking;
import com.rental.rental_system.repository.PropertyRepository;
import com.rental.rental_system.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.rental.rental_system.repository.BookingRepository;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Controller
public class AdminController {

    private final PropertyRepository repo;
    private final BookingRepository bookingRepo;
    private final UserRepository userRepo;

    public AdminController(PropertyRepository repo, BookingRepository bookingRepo, UserRepository userRepo) {
        this.repo = repo;
        this.bookingRepo = bookingRepo;
        this.userRepo = userRepo;
    }

    // ADMIN DASHBOARD
    @GetMapping("/admin")
    public String adminDashboard(@RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) String type,
                                 @RequestParam(required = false) String location,
                                 HttpSession session,
                                 Model model) {

        if (session.getAttribute("admin") == null) {
            return "redirect:/login";
        }

        model.addAttribute("locations", repo.findAllLocations());

        // 🔥 FILTER LOGIC
        if (keyword != null && !keyword.isEmpty()) {
            model.addAttribute("list", repo.search(keyword));
        }
        else if (type != null && !type.isEmpty() && location != null && !location.isEmpty()) {
            model.addAttribute("list", repo.findByTypeAndLocation(type, location));
        }
        else if (type != null && !type.isEmpty()) {
            model.addAttribute("list", repo.findByType(type));
        }
        else if (location != null && !location.isEmpty()) {
            model.addAttribute("list", repo.findByLocation(location));
        }
        else {
            model.addAttribute("list", repo.findAll());
        }

        // 🔥 DASHBOARD DATA
        List<Property> allProperties = repo.findAll();
        var allBookings = bookingRepo.findAll();

        int totalProperties = allProperties.size();
        int totalBookings = allBookings.size();

        // ✅ IMPROVED: Revenue calculation based on nights booked
        double totalRevenue = 0;
        for (var b : allBookings) {
            Property p = repo.findById(b.getPropertyId()).orElse(null);
            if (p != null && b.getStartDate() != null && b.getEndDate() != null) {
                long nights = ChronoUnit.DAYS.between(b.getStartDate(), b.getEndDate());
                totalRevenue += p.getPrice() * nights;
            }
        }

        // ✅ ENRICHED: Create detailed booking data with names and dates (sorted by latest first)
        List<Map<String, Object>> bookingDetails = new ArrayList<>();
        for (Booking b : allBookings) {
            Property p = repo.findById(b.getPropertyId()).orElse(null);
            User u = userRepo.findById(b.getUserId()).orElse(null);
            if (p != null && u != null && b.getStartDate() != null && b.getEndDate() != null) {
                Map<String, Object> detail = new HashMap<>();
                detail.put("propertyName", p.getName());
                detail.put("userName", u.getName());
                detail.put("userEmail", u.getEmail());
                detail.put("startDate", b.getStartDate());
                detail.put("endDate", b.getEndDate());
                detail.put("nights", ChronoUnit.DAYS.between(b.getStartDate(), b.getEndDate()));
                detail.put("bookingId", b.getId());
                bookingDetails.add(detail);
            }
        }
        // Sort by latest bookings first
        bookingDetails.sort((a, b) -> {
            java.time.LocalDate dateA = (java.time.LocalDate) a.get("startDate");
            java.time.LocalDate dateB = (java.time.LocalDate) b.get("startDate");
            return dateB.compareTo(dateA);
        });
        long flats = allProperties.stream()
                .filter(p -> p.getType().contains("BHK"))
                .count();

        long villas = allProperties.stream()
                .filter(p -> p.getType().equalsIgnoreCase("Villa"))
                .count();

        long penthouses = allProperties.stream()
                .filter(p -> p.getType().equalsIgnoreCase("Penthouse"))
                .count();

        // SEND TO UI
        model.addAttribute("totalProperties", totalProperties);
        model.addAttribute("totalBookings", totalBookings);
        model.addAttribute("totalRevenue", totalRevenue);

        model.addAttribute("flats", flats);
        model.addAttribute("villas", villas);
        model.addAttribute("penthouses", penthouses);

        // ✅ ENHANCED: Pass enriched booking details instead of raw bookings
        model.addAttribute("bookingDetails", bookingDetails);
        
        // ✅ NEW: For property-wise booking display
        Map<Integer, List<Map<String, Object>>> bookingsByProperty = new HashMap<>();
        for (Map<String, Object> detail : bookingDetails) {
            // Find property for this booking by matching with properties in list
            Property matchedProperty = null;
            for (Property p : (List<Property>) model.getAttribute("list")) {
                if (p != null && p.getName().equals(detail.get("propertyName"))) {
                    matchedProperty = p;
                    break;
                }
            }
            if (matchedProperty != null) {
                bookingsByProperty.computeIfAbsent(matchedProperty.getId(), k -> new ArrayList<>()).add(detail);
            }
        }
        model.addAttribute("bookingsByProperty", bookingsByProperty);

        return "admin";
    }

    // SHOW ADD FORM
    @GetMapping("/admin/add")
    public String showForm(HttpSession session, Model model) {

        if (session.getAttribute("admin") == null) {
            return "redirect:/login";
        }

        model.addAttribute("property", new Property());
        return "admin-add";
    }

    // SAVE PROPERTY
    @PostMapping("/admin/save")
    public String saveProperty(@ModelAttribute Property property, HttpSession session) {

        if (session.getAttribute("admin") == null) {
            return "redirect:/login";
        }

        repo.save(property);
        return "redirect:/admin";
    }

    // DELETE PROPERTY
    @GetMapping("/admin/delete/{id}")
    public String delete(@PathVariable int id) {
        repo.deleteById(id);
        return "redirect:/admin";
    }

    @GetMapping("/admin/edit/{id}")
    public String editProperty(@PathVariable int id, HttpSession session, Model model) {

        if (session.getAttribute("admin") == null) {
            return "redirect:/login";
        }

        Property p = repo.findById(id).orElse(null);
        model.addAttribute("property", p);

        return "admin-add";
    }

    @PostMapping("/admin/update")
    public String updateProperty(@ModelAttribute Property property, HttpSession session) {

        if (session.getAttribute("admin") == null) {
            return "redirect:/login";
        }

        repo.save(property); // updates existing

        return "redirect:/admin";
    }
}