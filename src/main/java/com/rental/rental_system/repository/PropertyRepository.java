package com.rental.rental_system.repository;

import com.rental.rental_system.model.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface PropertyRepository extends JpaRepository<Property, Integer> {
    List<Property> findByType(String type);
    List<Property> findByLocation(String location);
    List<Property> findByPriceBetween(Double minPrice, Double maxPrice);

    List<Property> findByTypeAndLocation(String type, String location);
    @Query("SELECT DISTINCT p.location FROM Property p")
    List<String> findAllLocations();

    @Query("SELECT p FROM Property p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.location) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.type) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Property> search(@Param("keyword") String keyword);
}