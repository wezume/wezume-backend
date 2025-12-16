package com.example.vprofile.emailservices;

import java.util.Calendar;
import java.util.Date;

import com.example.vprofile.logincredentials.User;
import com.example.vprofile.placementLogin.PlacementLogin;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class VerificationToken {

    // --- ADDED: The enum definition ---
    public enum UserType {
        USER,
        PLACEMENT
    }

    private static final int EXPIRATION_TIME_IN_MINUTES = 60 * 24; // 24 hours

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;
    
    private Long userId;
    
    private Long placementLoginId;

    // --- ADDED: The userType field ---
    @Enumerated(EnumType.STRING)
    private UserType userType;

    private Date expiryDate;

    // --- CONSTRUCTORS ---
    public VerificationToken() {}

    // Constructor for a normal User token
    public VerificationToken(String token, User user) {
        this.token = token;
        this.userId = user.getId();
        this.placementLoginId = null;
        this.userType = UserType.USER; // Explicitly set the user type
        this.expiryDate = calculateExpiryDate();
    }

    // Constructor for a PlacementLogin token
    public VerificationToken(String token, PlacementLogin placementLogin) {
        this.token = token;
        this.placementLoginId = placementLogin.getId();
        this.userId = null;
        this.userType = UserType.PLACEMENT; // Explicitly set the user type
        this.expiryDate = calculateExpiryDate();
    }
    
    private Date calculateExpiryDate() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.MINUTE, EXPIRATION_TIME_IN_MINUTES);
        return new Date(cal.getTime().getTime());
    }
    
    public void updateToken(String newToken) {
        this.token = newToken;
        this.expiryDate = calculateExpiryDate();
    }

    // --- GETTERS ---
    public Long getId() { return id; }
    public String getToken() { return token; }
    public Long getUserId() { return userId; }
    public Long getPlacementLoginId() { return placementLoginId; }
    public UserType getUserType() { return userType; }
    public Date getExpiryDate() { return expiryDate; }
}