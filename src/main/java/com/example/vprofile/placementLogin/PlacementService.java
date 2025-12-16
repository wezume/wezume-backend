package com.example.vprofile.placementLogin;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.vprofile.emailservices.VerificationTokenService;

@Service
public class PlacementService {

    @Autowired
    private PlacementRepository placementRepository;

    @Autowired
    private VerificationTokenService verificationTokenService;

    public void signup(PlacementLogin placementLogin) {
        // Check if email exists
        if (placementRepository.existsByEmail(placementLogin.getEmail())) {
            throw new RuntimeException("Email already in use");
        }

        // Check if phone number exists
        if (placementRepository.existsByPhoneNumber(placementLogin.getPhoneNumber())) {
            throw new RuntimeException("Phone number already in use");
        }

        // Check if passwords match
        if (!placementLogin.getPassword().equals(placementLogin.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        // Generate role code (optional)
        String roleCode = generateRoleCode(placementLogin.getCollege());
        placementLogin.setJobid(roleCode);

        // Save user in the database
        placementRepository.save(placementLogin);
    }

   public PlacementLogin login(PlacementLogin loginRequest) {
    // Find user by email (assuming you're using email for authentication)
    Optional<PlacementLogin> placementLoginOptional = placementRepository.findByEmail(loginRequest.getEmail());

    // Check if user exists and passwords match
    if (placementLoginOptional.isPresent()) {
        PlacementLogin placementLogin = placementLoginOptional.get();
        if (placementLogin.getPassword().equals(loginRequest.getPassword())) {
            return placementLogin;  // Return the user details if login is successful
        }
    }
    return null;  // Return null if login fails
}

    private String generateRoleCode(String collegeName) {
        String baseCode = collegeName.length() >= 3 ? collegeName.substring(0, 3).toUpperCase() : collegeName.toUpperCase();
        Random random = new Random();
        int randomNum = 100 + random.nextInt(900);
        return baseCode + randomNum;
    }

      public List<PlacementLogin> getCollegeByJobRoleAcademy(String jobRole) {
        // Find all PlacementLogin records by jobRole
        return placementRepository.findByJobOption(jobRole);
    }

    // Get all placement drive details for the "placementDrive" job role
    public List<PlacementLogin> getPlacementDriveDetailsByJobRole(String jobRole) {
        // Find all PlacementLogin records by jobRole
        return placementRepository.findByJobOption(jobRole);
    }

    public boolean existsByEmail(String email) {
    return placementRepository.existsByEmail(email);
}

public boolean existsByPhoneNumber(String phoneNumber) {
    return placementRepository.existsByPhoneNumber(phoneNumber);
}

public PlacementLogin registerNewPlacementAccount(PlacementLogin placementLogin) {
    placementLogin.setEnabled(false);

    // 1. Save the user. The 'savedUser' object now contains the database ID.
    PlacementLogin savedUser = placementRepository.save(placementLogin);

    // 2. Pass the 'savedUser' (with the ID) to the token service.
    verificationTokenService.createVerificationTokenForPlacement(savedUser);

    return savedUser;
}
    
    public void savePlacementLogin(PlacementLogin placementLogin) {
        placementRepository.save(placementLogin);
    }

    public Optional<PlacementLogin> findById(Long id) {
        return placementRepository.findById(id);
    }
}
