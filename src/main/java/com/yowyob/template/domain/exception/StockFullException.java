package com.yowyob.template.domain.exception;

public class StockFullException extends RuntimeException {
    public StockFullException(String message) {
        super(message);
    }
}