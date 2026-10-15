package com.momentum.backend.learntech;

// A course's assessment result. Score and scoreMax can be null even when
// status is set (API field reference).
public class ApiAssessment {

    public String status;      // passed, failed or attempted
    public Double score;
    public Double scoreMax;

}
