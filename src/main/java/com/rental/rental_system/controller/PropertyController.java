package com.rental.rental_system.controller;

import com.rental.rental_system.model.Property;
import com.rental.rental_system.model.Booking;
import com.rental.rental_system.model.User;
import com.rental.rental_system.repository.PropertyRepository;
import com.rental.rental_system.repository.BookingRepository;
import com.rental.rental_system.service.BookingService;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Controller
public class PropertyController {

    private final PropertyRepository repo;
    private final BookingRepository bookingRepo;
    private final BookingService bookingService;

    public PropertyController(PropertyRepository repo, BookingRepository bookingRepo, BookingService bookingService) {
        this.repo = repo;
        this.bookingRepo = bookingRepo;
        this.bookingService = bookingService;
    }

    // ===============================
    // SHOW ALL PROPERTIES (WITH FILTERS)
    // ===============================
    @GetMapping("/properties")
    public String viewProperties(@RequestParam(required = false) String type,
                                 @RequestParam(required = false) String location,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) Double minPrice,
                                 @RequestParam(required = false) Double maxPrice,
                                 Model model,
                                 HttpSession session) {

        Object user = session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", user);
        model.addAttribute("locations", repo.findAllLocations());

        // FILTER LOGIC (CLEAN VERSION)
        if (keyword != null && !keyword.isEmpty()) {
            model.addAttribute("list", repo.search(keyword));
        }
        else if (minPrice != null && maxPrice != null) {
            model.addAttribute("list", repo.findByPriceBetween(minPrice, maxPrice));
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

        return "properties";
    }

    // ===============================
    // PROPERTY DETAILS PAGE (WITH DATE SELECTION)
    // ===============================
    @GetMapping("/property/{id}")
    public String viewProperty(@PathVariable int id,
                               Model model,
                               HttpSession session) {

        Object obj = session.getAttribute("user");

        if (obj == null) {
            return "redirect:/login";
        }

        User user = (User) obj;

        Property p = repo.findById(id).orElse(null);

        if (p == null) {
            return "redirect:/properties";
        }

        // ✅ NEW: Get all bookings for this property to show on calendar
        var bookings = bookingService.getPropertyBookings(id);
        
        model.addAttribute("p", p);
        model.addAttribute("bookings", bookings);
        model.addAttribute("minDate", LocalDate.now().plusDays(1)); // Tomorrow is earliest start date

        return "property-details";
    }


    // ===============================
    // CHECK DATE AVAILABILITY (AJAX)
    // ===============================
    @GetMapping("/check-availability/{id}")
    @ResponseBody
    public String checkAvailability(@PathVariable int id,
                                    @RequestParam String startDate,
                                    @RequestParam String endDate) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate start = LocalDate.parse(startDate, formatter);
            LocalDate end = LocalDate.parse(endDate, formatter);

            // Validate dates
            String validationError = bookingService.validateDates(start, end);
            if (validationError != null) {
                return "{\"available\": false, \"message\": \"" + validationError + "\"}";
            }

            // Check availability
            boolean available = bookingService.isDateRangeAvailable(id, start, end);
            if (available) {
                return "{\"available\": true, \"message\": \"Dates available!\"}";
            } else {
                return "{\"available\": false, \"message\": \"This property is already booked for selected dates\"}";
            }
        } catch (Exception e) {
            return "{\"available\": false, \"message\": \"Invalid date format\"}";
        }
    }

    // ===============================
    // PAYMENT PAGE (WITH DATES)
    // ===============================
    @GetMapping("/payment/{id}")
    public String payment(@PathVariable int id,
                         @RequestParam String startDate,
                         @RequestParam String endDate,
                         HttpSession session,
                         Model model) {

        Object obj = session.getAttribute("user");

        if (obj == null) {
            return "redirect:/login";
        }

        User user = (User) obj;

        Property p = repo.findById(id).orElse(null);

        if (p == null) {
            return "redirect:/properties";
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate start = LocalDate.parse(startDate, formatter);
            LocalDate end = LocalDate.parse(endDate, formatter);

            // Store in session for payment confirmation
            session.setAttribute("bookingStartDate", start);
            session.setAttribute("bookingEndDate", end);
            session.setAttribute("bookingPropertyId", id);

            long nights = java.time.temporal.ChronoUnit.DAYS.between(start, end);
            double totalPrice = bookingService.calculateTotalPrice(p.getPrice(), start, end);

            model.addAttribute("p", p);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("nights", nights);
            model.addAttribute("totalPrice", totalPrice);

            return "payment";
        } catch (Exception e) {
            return "redirect:/properties?error=invalid_dates";
        }
    }

    @PostMapping("/pay/{id}")
    public String completePayment(@PathVariable int id,
                                  HttpSession session) {

        Object obj = session.getAttribute("user");

        if (obj == null) {
            return "redirect:/login";
        }

        User user = (User) obj;

        LocalDate startDate = (LocalDate) session.getAttribute("bookingStartDate");
        LocalDate endDate = (LocalDate) session.getAttribute("bookingEndDate");

        if (startDate == null || endDate == null) {
            return "redirect:/properties?error=invalid_session";
        }

        // Create booking with validation
        Booking booking = bookingService.createBooking(user.getId(), id, startDate, endDate);

        if (booking == null) {
            return "redirect:/property/" + id + "?error=booking_failed";
        }

        // Clear session
        session.removeAttribute("bookingStartDate");
        session.removeAttribute("bookingEndDate");
        session.removeAttribute("bookingPropertyId");

        return "payment-success";
    }

    // ===============================
    // MY BOOKINGS (WITH DATE RANGES)
    // ===============================
    @GetMapping("/my-bookings")
    public String myBookings(Model model, HttpSession session) {

        Object obj = session.getAttribute("user");

        if (obj == null) {
            return "redirect:/login";
        }

        User user = (User) obj;

        var bookings = bookingRepo.findByUserId(user.getId());

        // ✅ NEW: Create a map of booking details with dates
        var bookingDetails = new java.util.ArrayList<java.util.Map<String, Object>>();

        for (Booking b : bookings) {
            Property p = repo.findById(b.getPropertyId()).orElse(null);
            if (p != null) {
                var detail = new java.util.HashMap<String, Object>();
                detail.put("property", p);
                detail.put("booking", b);
                detail.put("nights", b.getNumberOfNights());
                bookingDetails.add(detail);
            }
        }

        model.addAttribute("bookings", bookingDetails);

        return "bookings";
    }

    // ===============================
    // CANCEL BOOKING
    // ===============================
    @GetMapping("/cancel/{id}")
    public String cancelBooking(@PathVariable int id,
                                HttpSession session) {

        Object obj = session.getAttribute("user");

        if (obj == null) {
            return "redirect:/login";
        }

        User user = (User) obj;

        // Find booking by ID and verify it belongs to the user
        var booking = bookingRepo.findById(id);

        if (booking.isPresent() && booking.get().getUserId() == user.getId()) {
            bookingRepo.delete(booking.get());
        }

        return "redirect:/my-bookings";
    }
}