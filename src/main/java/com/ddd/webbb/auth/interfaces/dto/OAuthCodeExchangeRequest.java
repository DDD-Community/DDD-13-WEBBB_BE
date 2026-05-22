package com.ddd.webbb.auth.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public record OAuthCodeExchangeRequest(@NotBlank String code) {}
