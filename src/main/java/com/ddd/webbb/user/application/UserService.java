package com.ddd.webbb.user.application;

import com.ddd.webbb.global.common.exception.AppException;
import com.ddd.webbb.global.common.exception.ErrorCode;
import com.ddd.webbb.user.domain.User;
import com.ddd.webbb.user.domain.UserRepository;
import com.ddd.webbb.user.infrastructure.UserRepositoryImpl;
import com.ddd.webbb.user.interfaces.dto.UserCreateRequest;
import com.ddd.webbb.user.interfaces.dto.UserListResponse;
import com.ddd.webbb.user.interfaces.dto.UserResponse;
import com.ddd.webbb.user.interfaces.dto.UserUpdateRequest;
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

    public UserResponse getUser(UUID publicId) {
        User user =
                userRepository
                        .findByPublicIdAndDeletedAtIsNull(publicId)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.from(user);
    }

    public UserListResponse getUsers(Long cursor, int size) {
        List<User> users = userRepositoryImpl.findByCursor(cursor, size);
        Long nextCursor = users.size() == size ? users.get(users.size() - 1).getId() : null;
        return new UserListResponse(users.stream().map(UserResponse::from).toList(), nextCursor);
    }

    @Transactional
    public UserResponse updateUser(UUID publicId, UserUpdateRequest request) {
        User user =
                userRepository
                        .findByPublicIdAndDeletedAtIsNull(publicId)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.update(request.nickname(), request.jobType(), request.careerLevel());
        return UserResponse.from(user);
    }

    @Transactional
    public void withdrawUser(UUID publicId) {
        User user =
                userRepository
                        .findByPublicIdAndDeletedAtIsNull(publicId)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.withdraw();
    }
}
