package com.momentum.backend.model;

// Holds what the app sends for emailed codes: two-step login, password reset,
// and turning two-step login on or off. Each route only uses the fields it needs.
public class CodeRequest {

    public String email;
    public String code;
    public String newPassword;
    public String password;

}
