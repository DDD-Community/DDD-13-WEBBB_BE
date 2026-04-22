package com.dnd.webbb.user.interfaces.dto;

import java.util.List;

public record UserListResponse(List<UserResponse> users, Long nextCursor) {}
