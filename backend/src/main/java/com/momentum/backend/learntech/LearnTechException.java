package com.momentum.backend.learntech;

// Something went wrong talking to LearnTech. The message says what,
// so it can be written to the activity log for the admin (FR-17).
public class LearnTechException extends Exception {

    public LearnTechException(String message) {
        super(message);
    }
}
