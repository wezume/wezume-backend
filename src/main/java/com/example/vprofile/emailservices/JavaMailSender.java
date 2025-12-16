package com.example.vprofile.emailservices; // It's good practice to have a 'controller' package

import java.util.Date; // Import the UserType enum
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import com.example.vprofile.emailservices.VerificationToken.UserType;
import com.example.vprofile.logincredentials.User;
import com.example.vprofile.logincredentials.UserService;
import com.example.vprofile.placementLogin.PlacementLogin;
import com.example.vprofile.placementLogin.PlacementService;

@Controller
@RequestMapping("/api")
public class JavaMailSender { // Renamed from JavaMailSender for clarity

    @Autowired
    private VerificationTokenService tokenService;

    @Autowired
    private UserService userService;

    @Autowired
    private PlacementService placementService;

    @GetMapping("/verify-email")
    public RedirectView verifyUserEmail(@RequestParam("token") String token) {
        VerificationToken verificationToken = tokenService.getVerificationToken(token);

        // 1. Check if token exists and is for the correct user type
        if (verificationToken == null || verificationToken.getUserType() != UserType.USER) {
            return new RedirectView("https://wezume.in/error.html?reason=invalid_token");
        }

        // 2. Check if token is expired
        if (verificationToken.getExpiryDate().before(new Date())) {
            tokenService.deleteToken(verificationToken); // Clean up expired token
            return new RedirectView("https://wezume.in/error.html?reason=expired_token");
        }

        // 3. Fetch the user from the database using the ID from the token
        Optional<User> userOptional = userService.findById(verificationToken.getUserId());
        if (userOptional.isEmpty()) {
            tokenService.deleteToken(verificationToken);
            return new RedirectView("https://wezume.in/error.html?reason=user_not_found");
        }

        // 4. Activate the user and delete the token to make it one-time use
        User user = userOptional.get();
        user.setEnabled(true);
        userService.saveUser(user);
        tokenService.deleteToken(verificationToken); // Enforce one-time use

        return new RedirectView("https://wezume.in/success.html");
    }

    // Inside VerificationController.java
    @GetMapping("/verify/placement/{token}")
    public RedirectView verifyPlacementEmail(@PathVariable String token) {
        VerificationToken verificationToken = tokenService.getVerificationToken(token);

        // 1. Check if token exists and is for the correct user type
        if (verificationToken == null || verificationToken.getUserType() != UserType.PLACEMENT) {
            return new RedirectView("https://wezume.in/error.html?reason=invalid_token");
        }

        // 2. Check if token is expired
        if (verificationToken.getExpiryDate().before(new Date())) {
            tokenService.deleteToken(verificationToken);
            return new RedirectView("https://wezume.in/error.html?reason=expired_token");
        }

        // --- THIS IS THE FIX ---
        // Use getPlacementLoginId() to fetch the correct user.
        Optional<PlacementLogin> placementOptional = placementService.findById(verificationToken.getPlacementLoginId());

        if (placementOptional.isEmpty()) {
            tokenService.deleteToken(verificationToken);
            return new RedirectView("https://wezume.in/error.html?reason=user_not_found");
        }

        // 4. Activate the user and delete the token
        PlacementLogin placementLogin = placementOptional.get();
        placementLogin.setEnabled(true);
        placementService.savePlacementLogin(placementLogin);
        tokenService.deleteToken(verificationToken);

        return new RedirectView("https://wezume.in/success.html");
    }
}
