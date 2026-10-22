package com.example.momentum.model;

// One check-in a learner logged on a goal. Matches the CheckIns table.
public class CheckIn {
    public int id;
    public String note;          // can be null
    public Integer valueLogged;  // can be null, e.g. minutes studied
    public String loggedAt;      // yyyy-MM-dd HH:mm:ss
}
