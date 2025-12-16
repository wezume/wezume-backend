package com.example.vprofile.logincredentials;

import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;

    @Column(name = "phone_number", unique = true) // Phone number should be unique
    private String phoneNumber;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
    }

    @NotNull
    @Email
    @Column(unique = true) // Email should be unique
    private String email;

    private String password;
    private String jobOption;

    @Lob
    @Column(name = "profile_pic", columnDefinition = "LONGBLOB")
    private byte[] profilePic;

    private String currentRole;
    private String experience; 
    private String industry;
    private String Profilepicurl;
    private String currentEmployer;
    private String keySkills;
    private String college;
    private String jobid;
    private String city;
    private Integer establishedYear; 

    private boolean enabled;

    // Constructors
    public User(Long id, String firstName, String lastName, String email, String phoneNumber, String password,
            String jobOption, byte[] profilePic, String currentRole, String keySkills,
            String experience, String industry, String currentEmployer,String college, String jobid,
            String city, Integer establishedYear, boolean enabled,String Profilepicurl) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.Profilepicurl = Profilepicurl;
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.college = college;
        this.jobid = jobid;
        this.jobOption = jobOption;
        this.profilePic = profilePic;
        this.currentRole = currentRole;
        this.keySkills = keySkills;
        this.experience = experience;
        this.industry = industry;
        this.currentEmployer = currentEmployer;
        this.city = city;
        this.establishedYear = establishedYear;
        this.enabled = enabled;
    }

    public User() {
    }

    // Getters and Setters for new fields
    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Integer getEstablishedYear() {
        return establishedYear;
    }

    public void setEstablishedYear(Integer establishedYear) {
        this.establishedYear = establishedYear;
    }

    // Existing Getters and Setters remain unchanged

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getJobOption() {
        return jobOption;
    }

    public void setJobOption(String jobOption) {
        this.jobOption = jobOption;
    }

    public byte[] getProfilePic() {
        return profilePic;
    }

    public void setProfilePic(byte[] profilePic) {
        this.profilePic = profilePic;
    }

    public String getCurrentRole() {
        return currentRole;
    }

    public void setCurrentRole(String currentRole) {
        this.currentRole = currentRole;
    }

    public String getKeySkills() {
        return keySkills;
    }

    public void setKeySkills(String keySkills) {
        this.keySkills = keySkills;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getCurrentEmployer() {
        return currentEmployer;
    }

    public void setCurrentEmployer(String currentEmployer) {
        this.currentEmployer = currentEmployer;
    }


    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getCollege() {
        return college;
    }   
    public void setCollege(String college) {
        this.college = college;
    }
    public String getJobId() {
        return jobid;
    }
    public void setJobId(String jobid) {
        this.jobid = jobid;   
    }

    public String getProfilepicurl() {
        return Profilepicurl;
    }

    public void setProfilepicurl(String profilepicurl) {
        Profilepicurl = profilepicurl;
    }

    

}
