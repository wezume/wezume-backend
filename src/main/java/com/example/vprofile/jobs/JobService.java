package com.example.vprofile.jobs;

import java.util.Arrays;
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
    public void seedIfEmpty() {
        if (jobRepository.count() > 0) {
            return;
        }
        List<Job> seed = Arrays.asList(
                buildJob("Strategy Associate", "PE-Backed Firm, Bangalore", "0-1 yr", "₹6-8 LPA", "WZ-10234", 1),
                buildJob("Brand Executive", "Wipro Consumer Care", "0-2 yrs", "₹5-7 LPA", "WZ-10235", 2),
                buildJob("HR Associate", "MUFG", "0-1 yr", "₹7-9 LPA", "WZ-10236", 3),
                buildJob("Customer Growth Specialist", "SurveySparrow", "1-3 yrs", "₹6-10 LPA", "WZ-10237", 4),
                buildJob("IT Global Sales Associate", "Multicoreware", "0-2 yrs", "₹5-8 LPA", "WZ-10238", 5),
                buildJob("Campus Partnerships Manager", "Reva University Network", "2-4 yrs", "₹8-12 LPA", "WZ-10239", 6)
        );
        jobRepository.saveAll(seed);
    }

    private Job buildJob(String name, String company, String exp, String ctc, String jobId, int sortOrder) {
        Job job = new Job();
        job.setName(name);
        job.setCompany(company);
        job.setExp(exp);
        job.setCtc(ctc);
        job.setJobId(jobId);
        job.setSortOrder(sortOrder);
        return job;
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAllByOrderBySortOrderAsc();
    }

    public Job addJob(Job job) {
        int nextOrder = jobRepository.findAllByOrderBySortOrderAsc().stream()
                .map(Job::getSortOrder)
                .filter(o -> o != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;
        job.setId(null);
        job.setSortOrder(nextOrder);
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
