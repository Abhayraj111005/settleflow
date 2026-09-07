package com.settleflow.controller;

import com.settleflow.entity.InvalidSettlementStateTransitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class SettlementExceptionHandler {

    @ExceptionHandler(InvalidSettlementStateTransitionException.class)
    public ResponseEntity<Map<String, String>> handleInvalidTransition(
            InvalidSettlementStateTransitionException exception) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error",
                        exception.getMessage()
                ));
    }
}