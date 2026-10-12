package com.momentum.backend.model;

// One points award, as read from the PointsTransactions table.
public class PointsTransaction {

    public int transactionId;
    public int userId;
    public int points;
    public String reason;
    public Integer referenceId;
    public String earnedAt;
    public String monthKey;

}
