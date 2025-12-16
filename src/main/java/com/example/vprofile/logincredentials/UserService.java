package com.example.vprofile.logincredentials;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.vprofile.emailservices.VerificationTokenService;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationTokenService tokenService;

    public User saveUser(User user) {
        return userRepository.save(user); // Save user details to the database
    }

    public boolean authenticateUser(String email, String password) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        // Check if user exists and the password matches
        if (userOptional.isPresent()) {
            User user = userOptional.get(); // Retrieve the User object
            return user.getPassword().equals(password);
        }
        return false; // User not found or password doesn't match
    }

    public boolean isEmailExists(String email) {
        return userRepository.existsByEmail(email);  // This will return true if email exists
    }

    public boolean isPhoneExists(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    public User updateUser(Long id, User updatedUser) {
        return userRepository.findById(id).map(existingUser -> {
            // Update fields if they are not null
            if (updatedUser.getFirstName() != null) {
                existingUser.setFirstName(updatedUser.getFirstName());
            }
            if (updatedUser.getLastName() != null) {
                existingUser.setLastName(updatedUser.getLastName());
            }
            if (updatedUser.getPhoneNumber() != null) {
                existingUser.setPhoneNumber(updatedUser.getPhoneNumber());
            }
            if (updatedUser.getEmail() != null) {
                existingUser.setEmail(updatedUser.getEmail());
            }
            if (updatedUser.getPassword() != null) {
                existingUser.setPassword(updatedUser.getPassword());
            }
            if (updatedUser.getJobOption() != null) {
                existingUser.setJobOption(updatedUser.getJobOption());
            }
            if (updatedUser.getProfilePic() != null) {
                existingUser.setProfilePic(updatedUser.getProfilePic());
            }
            if (updatedUser.getCurrentRole() != null) {
                existingUser.setCurrentRole(updatedUser.getCurrentRole());
            }
            if (updatedUser.getKeySkills() != null) {
                existingUser.setKeySkills(updatedUser.getKeySkills());
            }
            if (updatedUser.getExperience() != null) {
                existingUser.setExperience(updatedUser.getExperience());
            }
            if (updatedUser.getIndustry() != null) {
                existingUser.setIndustry(updatedUser.getIndustry());
            }
            if (updatedUser.getCurrentEmployer() != null) {
                existingUser.setCurrentEmployer(updatedUser.getCurrentEmployer());
            }
            if (updatedUser.getCity() != null) {
                existingUser.setCity(updatedUser.getCity());
            }
            if (updatedUser.getEstablishedYear() != null) {
                existingUser.setEstablishedYear(updatedUser.getEstablishedYear());
            }
            existingUser.setEnabled(updatedUser.isEnabled());
            return userRepository.save(existingUser);
        }).orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Inside your UserService.java
    public User registerNewUser(User user) {
        user.setEnabled(false);

        // 1. Save the user. The 'savedUser' object now contains the database ID.
        User savedUser = userRepository.save(user);

        // 2. Pass the 'savedUser' (with the ID) to the token service.
        tokenService.createVerificationTokenForUser(savedUser);

        return savedUser;
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean updatePasswordByEmail(String email, String newPassword) {
        // Find the user by email
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setPassword(newPassword); // Update the password
            userRepository.save(user); // Save the updated user
            return true;
        } else {
            return false; // If user not found
        }
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public boolean existsByPhoneNumber(String phoneNumber) {
    return userRepository.existsByPhoneNumber(phoneNumber);
}

}
