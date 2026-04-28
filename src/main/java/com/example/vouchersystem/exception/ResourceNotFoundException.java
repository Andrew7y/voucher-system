package com.example.vouchersystem.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message){
        super(message);
    }
}
