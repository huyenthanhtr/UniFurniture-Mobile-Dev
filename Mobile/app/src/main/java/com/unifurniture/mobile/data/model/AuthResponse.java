package com.unifurniture.mobile.data.model;

public class AuthResponse {
    public String token;
    public CustomerDto customer;
    public String message;

    // Set by AuthRepository: true when the HTTP response was 2xx.
    // Lets callers tell a successful register/OTP (which carry no token) apart from an error.
    public transient boolean success;
}
