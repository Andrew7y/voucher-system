package com.example.vouchersystem.exception;

public class BusinessRuleException extends RuntimeException{
    public BusinessRuleException(String message){
        super(message);
    }
}
