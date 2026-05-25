package com.serasa.flowengine.api;

import com.serasa.flowengine.api.dto.Responses;
import com.serasa.flowengine.domain.engine.BlockRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FlowController.FlowNotFoundException.class)
    public ResponseEntity<Responses.ErrorResponse> handleNotFound(FlowController.FlowNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Responses.ErrorResponse.of("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(BlockRegistry.UnknownBlockTypeException.class)
    public ResponseEntity<Responses.ErrorResponse> handleUnknownBlock(BlockRegistry.UnknownBlockTypeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Responses.ErrorResponse.of("UNKNOWN_BLOCK_TYPE", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Responses.ErrorResponse> handleIllegalArg(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Responses.ErrorResponse.of("INVALID_FLOW", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Responses.ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Responses.ErrorResponse.of("VALIDATION_ERROR", details));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Responses.ErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Responses.ErrorResponse.of("INTERNAL_ERROR", ex.getMessage()));
    }
}
