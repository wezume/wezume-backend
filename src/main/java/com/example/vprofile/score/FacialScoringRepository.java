package com.example.vprofile.score;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FacialScoringRepository extends JpaRepository<FacialScoring, Long> {
    Optional<FacialScoring> findByVideoId(Long videoId);
    List<FacialScoring> findAllByVideoIdIn(List<Long> videoIds);
}
