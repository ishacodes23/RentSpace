package com.rental.rental_system.service;

import com.rental.rental_system.model.Booking;
import com.rental.rental_system.repository.BookingRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    /**
     * Check if dates are available for booking
     * @param propertyId The property to check
     * @param startDate Check-in date
     * @param endDate Check-out date
     * @return true if available, false if booked
     */
    public boolean isDateRangeAvailable(int propertyId, LocalDate startDate, LocalDate endDate) {
        List<Booking> overlappingBookings = bookingRepository.findOverlappingBookings(propertyId, startDate, endDate);
        return overlappingBookings.isEmpty();
    }

    /**
     * Get all booked dates for a property
     * @param propertyId The property
     * @return List of bookings with date ranges
     */
    public List<Booking> getPropertyBookings(int propertyId) {
        return bookingRepository.findBookingsByPropertyOrderByDate(propertyId);
    }

    /**
     * Validate booking dates
     * @param startDate Check-in date
     * @param endDate Check-out date
     * @return Error message if invalid, null if valid
     */
    public String validateDates(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();

        // Check if start date is in the past
        if (startDate.isBefore(today)) {
            return "Start date cannot be in the past";
        }

        // Check if end date is before or equal to start date
        if (endDate.isBefore(startDate) || endDate.equals(startDate)) {
            return "End date must be after start date";
        }

        // Check if booking is at least 1 night
        long nights = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        if (nights < 1) {
            return "Booking must be for at least 1 night";
        }

        return null; // All validations passed
    }

    /**
     * Create a booking with full validation
     * @param userId User making the booking
     * @param propertyId Property to book
     * @param startDate Check-in date
     * @param endDate Check-out date
     * @return Booking if successful, null if validation fails
     */
    public Booking createBooking(int userId, int propertyId, LocalDate startDate, LocalDate endDate) {
        // Validate dates first
        String validationError = validateDates(startDate, endDate);
        if (validationError != null) {
            System.out.println("Validation error: " + validationError);
            return null;
        }

        // Check if dates are available
        if (!isDateRangeAvailable(propertyId, startDate, endDate)) {
            System.out.println("Date range not available");
            return null;
        }

        // Create and save booking
        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setPropertyId(propertyId);
        booking.setStartDate(startDate);
        booking.setEndDate(endDate);
        booking.setBookingDate(LocalDate.now());

        return bookingRepository.save(booking);
    }

    /**
     * Calculate total price for a booking
     * @param pricePerNight The nightly rate
     * @param startDate Check-in date
     * @param endDate Check-out date
     * @return Total price including GST
     */
    public double calculateTotalPrice(double pricePerNight, LocalDate startDate, LocalDate endDate) {
        long nights = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        double totalRent = pricePerNight * nights;
        double deposit = pricePerNight * 2;
        double maintenance = 2000 * nights;
        double gst = totalRent * 0.05;

        return totalRent + deposit + maintenance + gst;
    }
}
