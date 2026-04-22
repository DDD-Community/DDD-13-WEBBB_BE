package com.dnd.webbb.user.application;

import com.dnd.webbb.global.common.exception.AppException;
import com.dnd.webbb.global.common.exception.ErrorCode;
import com.dnd.webbb.user.domain.User;
import com.dnd.webbb.user.domain.UserRepository;
import com.dnd.webbb.user.infrastructure.UserRepositoryImpl;
import com.dnd.webbb.user.interfaces.dto.UserCreateRequest;
import com.dnd.webbb.user.interfaces.dto.UserListResponse;
import com.dnd.webbb.user.interfaces.dto.UserResponse;
import com.dnd.webbb.user.interfaces.dto.UserUpdateRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserRepositoryImpl userRepositoryImpl;

    public UserService(UserRepository userRepository, UserRepositoryImpl userRepositoryImpl) {
        this.userRepository = userRepository;
        this.userRepositoryImpl = userRepositoryImpl;
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.existsByEmailAndDeletedAtIsNull(request.email())) {
            throw new AppException(ErrorCode.DUPLICATED_EMAIL);
        }
        User user = User.create(request.email(), request.nickname());
        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse getUser(UUID id) {
        User user =
                userRepository
                        .findByPublicIdAndDeletedAtIsNull(id)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.from(user);
    }

    public UserListResponse getUsers(Long cursor, int size) {
        List<User> users = userRepositoryImpl.findByCursor(cursor, size);
        Long nextCursor = users.size() == size ? users.get(users.size() - 1).getId() : null;
        return new UserListResponse(users.stream().map(UserResponse::from).toList(), nextCursor);
    }

    @Transactional
    public UserResponse updateUser(UUID id, UserUpdateRequest request) {
        User user =
                userRepository
                        .findByPublicIdAndDeletedAtIsNull(id)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.update(request.nickname(), request.status());
        return UserResponse.from(user);
    }

    @Transactional
    public void withdrawUser(UUID id) {
        User user =
                userRepository
                        .findByPublicIdAndDeletedAtIsNull(id)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.withdraw();
    }
}
