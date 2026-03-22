package com.example.purchase.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardized error response for API exceptions.
 * Provides consistent error information across all endpoints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    /**
     * Timestamp when the error occurred.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;
    
    /**
     * HTTP status code (e.g., 400, 404, 500).
     */
    private int status;
    
    /**
     * Short error name/title (e.g., "Bad Request", "Not Found").
     */
    private String error;
    
    /**
     * Detailed error message for the client.
     */
    private String message;
    
    /**
     * API path where the error occurred.
     */
    private String path;
    
    /**
     * Optional list of validation errors (for field-level validation).
     */
    private List<FieldError> errors;
    
    /**
     * Represents a field-level validation error.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }
}
