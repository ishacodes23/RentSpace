package com.rental.rental_system.repository;

import com.rental.rental_system.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Integer> {

    List<Booking> findByUserId(int userId);

    boolean existsByUserIdAndPropertyId(int userId, int propertyId);

    // ✅ SAFE FETCH (instead of direct delete)
    Optional<Booking> findByUserIdAndPropertyId(int userId, int propertyId);

    // ✅ NEW: Find all bookings for a property
    List<Booking> findByPropertyId(int propertyId);

    // ✅ NEW: Check for date overlap
    // A booking overlaps if:
    // - existingStartDate <= requestedEndDate AND
    // - existingEndDate >= requestedStartDate
    @Query("SELECT b FROM Booking b WHERE b.propertyId = :propertyId " +
           "AND b.startDate <= :endDate AND b.endDate >= :startDate")
    List<Booking> findOverlappingBookings(@Param("propertyId") int propertyId,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    // ✅ NEW: Get all bookings for a property with date range
    @Query("SELECT b FROM Booking b WHERE b.propertyId = :propertyId " +
           "ORDER BY b.startDate ASC")
    List<Booking> findBookingsByPropertyOrderByDate(@Param("propertyId") int propertyId);
}