package com.example.vprofile.analysis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProfanityService {

    @Autowired
    private ProfanityRepository profanityRepository;

    public Profanity saveProfanity(Long userId, Long videoId, Boolean containsProfanity) {
        Profanity profanity = new Profanity(userId, videoId, containsProfanity);
        return profanityRepository.save(profanity);
    }

    public Profanity getProfanityByUserAndVideo(Long userId, Long videoId) {
        return profanityRepository.findByUserIdAndVideoId(userId, videoId);
    }
}
