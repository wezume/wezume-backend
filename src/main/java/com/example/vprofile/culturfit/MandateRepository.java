package com.example.vprofile.culturfit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MandateRepository extends JpaRepository<Mandate, Integer> {
    List<Mandate> findByFunctionalArea(String functionalArea);
}
