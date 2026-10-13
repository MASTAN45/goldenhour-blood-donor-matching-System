package com.lifelink.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class RequestAlreadyFilledException extends RuntimeException {
    public RequestAlreadyFilledException() {
        super("REQUEST_ALREADY_FILLED");
    }
}