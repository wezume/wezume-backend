package com.example.vprofile.score;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TotalScoreRepository extends JpaRepository<TotalScore, Long> {
    Optional<TotalScore> findByVideoId(Long videoId);
    List<TotalScore> findAllByVideoIdIn(List<Long> videoIds);
}
