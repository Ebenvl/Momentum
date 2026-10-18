package com.example.momentum.model;

// A course from the catalogue, as synced from LearnTech.
public class Course {
    public int courseId;
    public String name;
    public Integer expectedMinutes;  // can be null
    public Integer pointsValue;      // admin override, can be null
}
