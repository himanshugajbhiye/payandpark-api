package com.payandpark.payandpark.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class ResourceNotSavedException extends RuntimeException{
    public ResourceNotSavedException(String message) {
        super(message);
    }
}
