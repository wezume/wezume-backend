package com.example.vprofile.placementLogin;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
public interface PlacementRepository extends JpaRepository<PlacementLogin, Long> {

    // Check if a user exists by email
    boolean existsByEmail(String email);

     boolean existsByPhoneNumber(String phoneNumber);

    // Find a user by email
     Optional<PlacementLogin> findByEmail(String email);

    List<PlacementLogin> findByJobOption(String jobOption);

}