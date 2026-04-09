package com.example.vprofile.culturfit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CultureFitRepository extends JpaRepository<CultureFitScore, Integer> {
    List<CultureFitScore> findByCandidateId(Integer candidateId);
}
