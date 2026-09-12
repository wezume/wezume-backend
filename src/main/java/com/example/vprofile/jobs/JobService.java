package com.example.vprofile.jobs;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class JobService {

    private static final Set<String> EDITABLE_FIELDS = Set.of("name", "company", "exp", "ctc", "jobId");

    @Autowired
    private JobRepository jobRepository;

    @PostConstruct
    public void initialize() {
        seedIfEmpty();
        backfillMissingCreatedAt();
    }

    private void seedIfEmpty() {
        if (jobRepository.count() > 0) {
            return;
        }
        // Oldest first here so they land at the bottom under the
        // newest-first (createdAt desc) sort once real posts are added.
        LocalDateTime base = LocalDateTime.now().minusMinutes(6);
        List<Job> seed = Arrays.asList(
                buildJob("Strategy Associate", "PE-Backed Firm, Bangalore", "0-1 yr", "₹6-8 LPA", "WZ-10234", base.plusMinutes(1)),
                buildJob("Brand Executive", "Wipro Consumer Care", "0-2 yrs", "₹5-7 LPA", "WZ-10235", base.plusMinutes(2)),
                buildJob("HR Associate", "MUFG", "0-1 yr", "₹7-9 LPA", "WZ-10236", base.plusMinutes(3)),
                buildJob("Customer Growth Specialist", "SurveySparrow", "1-3 yrs", "₹6-10 LPA", "WZ-10237", base.plusMinutes(4)),
                buildJob("IT Global Sales Associate", "Multicoreware", "0-2 yrs", "₹5-8 LPA", "WZ-10238", base.plusMinutes(5)),
                buildJob("Campus Partnerships Manager", "Reva University Network", "2-4 yrs", "₹8-12 LPA", "WZ-10239", base.plusMinutes(6))
        );
        jobRepository.saveAll(seed);
    }

    // One-time migration for rows that existed before createdAt was added
    // (this ran once against the already-live jobs table). Assigns
    // increasing past timestamps in id order so old posts keep their
    // relative order and sort below anything newly added from now on.
    private void backfillMissingCreatedAt() {
        List<Job> missing = jobRepository.findAll().stream()
                .filter(j -> j.getCreatedAt() == null)
                .sorted(Comparator.comparing(Job::getId))
                .toList();
        if (missing.isEmpty()) {
            return;
        }
        LocalDateTime base = LocalDateTime.now().minusDays(1);
        for (int i = 0; i < missing.size(); i++) {
            missing.get(i).setCreatedAt(base.plusMinutes(i));
        }
        jobRepository.saveAll(missing);
    }

    private Job buildJob(String name, String company, String exp, String ctc, String jobId, LocalDateTime createdAt) {
        Job job = new Job();
        job.setName(name);
        job.setCompany(company);
        job.setExp(exp);
        job.setCtc(ctc);
        job.setJobId(jobId);
        job.setCreatedAt(createdAt);
        return job;
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAllByOrderByCreatedAtDesc();
    }

    public Job addJob(Job job) {
        job.setId(null);
        job.setCreatedAt(LocalDateTime.now());
        return jobRepository.save(job);
    }

    public Job updateJobField(Long id, String field, String value) {
        if (!EDITABLE_FIELDS.contains(field)) {
            throw new IllegalArgumentException("Unknown field: " + field);
        }
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));
        switch (field) {
            case "name" -> job.setName(value);
            case "company" -> job.setCompany(value);
            case "exp" -> job.setExp(value);
            case "ctc" -> job.setCtc(value);
            case "jobId" -> job.setJobId(value);
            default -> throw new IllegalArgumentException("Unknown field: " + field);
        }
        return jobRepository.save(job);
    }

    public void deleteJob(Long id) {
        jobRepository.deleteById(id);
    }
}
