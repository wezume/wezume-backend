package com.example.vprofile.emailservices;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
   VerificationToken findByToken(String token);
    VerificationToken findByUserId(Long userId);
    VerificationToken findByPlacementLoginId(Long placementLoginId);
}
