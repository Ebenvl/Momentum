package com.momentum.backend.learntech;

import java.util.List;

// The whole reply from GET /learners.
public class LearnersResponse {

    public boolean success;
    public String generatedAt;
    public int count;
    public List<ApiLearner> learners;

}
