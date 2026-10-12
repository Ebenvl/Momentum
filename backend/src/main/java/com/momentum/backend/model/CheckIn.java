package com.momentum.backend.model;

// One check-in against a goal, as read from the CheckIns table.
public class CheckIn {

    public int checkinId;
    public int goalId;
    public int userId;
    public String loggedAt;
    public String note;
    public Integer valueLogged;

}
