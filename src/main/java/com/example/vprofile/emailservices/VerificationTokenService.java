package com.example.vprofile.emailservices;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.vprofile.logincredentials.User;
import com.example.vprofile.placementLogin.PlacementLogin;

@Service
public class VerificationTokenService {

    @Autowired
    private VerificationTokenRepository tokenRepository;

    @Autowired
    private EmailService emailService;

    public void createVerificationTokenForUser(User user) {
        VerificationToken existingToken = tokenRepository.findByUserId(user.getId());

        if (existingToken != null) {
            String newToken = UUID.randomUUID().toString();
            existingToken.updateToken(newToken);
            tokenRepository.save(existingToken);
            emailService.sendVerificationEmail(user.getEmail(), newToken, "user");
        } else {
            String token = UUID.randomUUID().toString();
            
            // --- THIS IS THE FIX ---
            // Pass the entire 'user' object, not just user.getId()
            VerificationToken verificationToken = new VerificationToken(token, user);
            
            tokenRepository.save(verificationToken);
            emailService.sendVerificationEmail(user.getEmail(), token, "user");
        }
    }

    public void createVerificationTokenForPlacement(PlacementLogin placementLogin) {
        VerificationToken existingToken = tokenRepository.findByPlacementLoginId(placementLogin.getId());

        if (existingToken != null) {
            String newToken = UUID.randomUUID().toString();
            existingToken.updateToken(newToken);
            tokenRepository.save(existingToken);
            emailService.sendVerificationEmail(placementLogin.getEmail(), newToken, "PlacementLogin");
        } else {
            String token = UUID.randomUUID().toString();
            
            // --- THIS IS THE FIX ---
            // Pass the entire 'placementLogin' object
            VerificationToken verificationToken = new VerificationToken(token, placementLogin);

            tokenRepository.save(verificationToken);
            emailService.sendVerificationEmail(placementLogin.getEmail(), token, "PlacementLogin");
        }
    }

    public VerificationToken getVerificationToken(String token) {
        return tokenRepository.findByToken(token);
    }
    
    public void deleteToken(VerificationToken token) {
        tokenRepository.delete(token);
    }
}