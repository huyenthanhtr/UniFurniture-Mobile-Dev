package com.unifurniture.mobile.data.model;

/**
 * Unified response for auth endpoints (login, verifyOtp).
 *
 * Server returns:
 *   - login:     { message, profile }
 *   - verifyOtp: { message, profile }
 *   - register:  { message }            (no profile — must go through OTP first)
 *   - forgotPw:  { message }
 *   - resetPw:   { message }
 */
public class AuthResponse {
    public String message;
    public ProfileDto profile;
}
