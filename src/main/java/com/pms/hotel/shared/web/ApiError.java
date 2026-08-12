package com.pms.hotel.shared.web;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, Object> errors) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, null);
    }

    public static ApiError of(int status, String error, String message, String path, Map<String, Object> errors) {
        return new ApiError(Instant.now(), status, error, message, path, errors);
    }
}
