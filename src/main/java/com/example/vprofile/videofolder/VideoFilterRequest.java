package com.example.vprofile.videofolder;

import java.util.List;

public class VideoFilterRequest {
    private String keySkills;
    private String keywords;
    private List<String> experience;
    private List<String> industry;
    private List<String> city;

    public VideoFilterRequest() {
    }

    public VideoFilterRequest(String keySkills,String keywords, List<String> experience, List<String> industry, List<String> city) {
        this.keySkills = keySkills;
        this.experience = experience;
        this.industry = industry;
        this.city = city;
        this.keywords = keywords;
    }

    public String getKeySkills() {
        return keySkills;
    }
    public void setKeySkills(String keySkills) {
        this.keySkills = keySkills;
    }
    public List<String> getExperience() {
        return experience;
    }
    public void setExperience(List<String> experience) {
        this.experience = experience;
    }
    public List<String> getIndustry() {
        return industry;
    }
    public void setIndustry(List<String> industry) {
        this.industry = industry;
    }
    public List<String> getCity() {
        return city;
    }
    public void setCity(List<String> city) {
        this.city = city;
    }
public String getKeywords() {
    return keywords;
}

public void setKeywords(String keywords) {
    this.keywords = keywords;
}
}
