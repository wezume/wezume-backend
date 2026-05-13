package com.example.vprofile.score;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeechScoreRepository extends JpaRepository<SpeechScore, Long> {
    Optional<SpeechScore> findByVideoId(Long videoId);
    List<SpeechScore> findAllByVideoIdIn(List<Long> videoIds);
}
