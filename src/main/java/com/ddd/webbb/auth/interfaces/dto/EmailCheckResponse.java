package com.ddd.webbb.auth.interfaces.dto;

public record EmailCheckResponse(boolean available) {

    public static EmailCheckResponse of(boolean available) {
        return new EmailCheckResponse(available);
    }
}
