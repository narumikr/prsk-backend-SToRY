package com.example.untitled.common.exception;

import com.example.untitled.common.dto.ErrorDetails;

import java.util.List;

public class BadRequestException extends RuntimeException {

    private final List<ErrorDetails> details;

    public BadRequestException(String message, List<ErrorDetails> details) {
        super(message);
        this.details = details;
    }

    public List<ErrorDetails> getDetails() { return this.details; }
}
