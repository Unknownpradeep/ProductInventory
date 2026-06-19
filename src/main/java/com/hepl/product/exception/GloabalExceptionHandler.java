package com.hepl.product.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.hepl.product.Payload.Response.ApiResponse;

@RestControllerAdvice
public class GloabalExceptionHandler {

    private void logError(Exception ex, jakarta.servlet.http.HttpServletRequest request) {
        ex.printStackTrace();
        try {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            ex.printStackTrace(pw);
            
            String uri = request != null ? request.getRequestURI() : "Unknown URI";
            String method = request != null ? request.getMethod() : "Unknown Method";
            
            java.io.File file = new java.io.File("C:\\Users\\acer\\Desktop\\PRODUCT\\api_error.txt");
            String errorLog = String.format("--- FAILURE LOGGED AT %s ---\nRequest: %s %s\nException Class: %s\nMessage: %s\nStack Trace:\n%s\n\n", 
                java.time.LocalDateTime.now(), method, uri, ex.getClass().getName(), ex.getMessage(), sw.toString());
                
            java.nio.file.Files.writeString(
                file.toPath(), 
                errorLog, 
                java.nio.file.StandardOpenOption.CREATE, 
                java.nio.file.StandardOpenOption.APPEND
            );
        } catch (Exception e) {
            System.err.println("Failed to write to api_error.txt: " + e.getMessage());
        }
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse> handleResourceNotFound(ResourceNotFoundException ex, jakarta.servlet.http.HttpServletRequest request){
        logError(ex, request);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            new ApiResponse(404, ex.getMessage(), null)
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handlValidation(MethodArgumentNotValidException ex, jakarta.servlet.http.HttpServletRequest request){
        logError(ex, request);
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.badRequest().body(
            new ApiResponse(400, "Validation failed", fieldErrors)
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, jakarta.servlet.http.HttpServletRequest request) {
        logError(ex, request);
        String message = "Database constraint violation occurred.";
        String rootMsg = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
        if (rootMsg != null) {
            if (rootMsg.contains("Duplicate entry") || rootMsg.toLowerCase().contains("duplicate")) {
                message = "A record with this unique attribute already exists.";
            } else if (rootMsg.toLowerCase().contains("foreign key") || rootMsg.toLowerCase().contains("referential integrity")) {
                message = "This record cannot be modified or deleted because it is referenced by another record.";
            }
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            new ApiResponse(409, message, null)
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, jakarta.servlet.http.HttpServletRequest request) {
        logError(ex, request);
        return ResponseEntity.badRequest().body(
            new ApiResponse(400, "Malformed JSON request body or invalid data type", null)
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, jakarta.servlet.http.HttpServletRequest request) {
        logError(ex, request);
        String msg = String.format("Parameter '%s' should be of type %s", ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "correct type");
        return ResponseEntity.badRequest().body(
            new ApiResponse(400, msg, null)
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse> handleMissingParams(MissingServletRequestParameterException ex, jakarta.servlet.http.HttpServletRequest request) {
        logError(ex, request);
        String msg = String.format("Missing required request parameter: %s", ex.getParameterName());
        return ResponseEntity.badRequest().body(
            new ApiResponse(400, msg, null)
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex, jakarta.servlet.http.HttpServletRequest request) {
        logError(ex, request);
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
            new ApiResponse(413, "Uploaded file size exceeds the maximum limit.", null)
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse> handleRuntime(RuntimeException ex, jakarta.servlet.http.HttpServletRequest request) {
        logError(ex, request);
        return ResponseEntity.badRequest().body(
            new ApiResponse(400, ex.getMessage(), null)
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGlobalException(Exception ex, jakarta.servlet.http.HttpServletRequest request) {
        logError(ex, request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            new ApiResponse(500, "An unexpected server error occurred: " + ex.getMessage(), null)
        );
    }
}
