package com.example.vprofile.culturfit;

import jakarta.persistence.*;

@Entity
@Table(name = "mandate")
public class Mandate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "jobid", nullable = false)
    private String jobid;

    @Column(name = "functional_area")
    private String functionalArea;

    public Mandate() {}

    public Mandate(String name, String jobid, String functionalArea) {
        this.name = name;
        this.jobid = jobid;
        this.functionalArea = functionalArea;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJobid() {
        return jobid;
    }

    public void setJobid(String jobid) {
        this.jobid = jobid;
    }

    public String getFunctionalArea() {
        return functionalArea;
    }

    public void setFunctionalArea(String functionalArea) {
        this.functionalArea = functionalArea;
    }
}
