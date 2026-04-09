package com.example.vprofile.culturfit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class RoleController {

    @Autowired
    private MandateRepository mandateRepository;

    /**
     * GET /api/roles - Get mandates (roles) from the database, optionally filtered by functional area
     */
    @GetMapping
    public ResponseEntity<List<Mandate>> getAllRoles(@org.springframework.web.bind.annotation.RequestParam(required = false) String functionalArea) {
        try {
            List<Mandate> mandates;
            if (functionalArea != null && !functionalArea.isEmpty()) {
                mandates = mandateRepository.findByFunctionalArea(functionalArea);
            } else {
                mandates = mandateRepository.findAll();
            }
            return ResponseEntity.ok(mandates);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
