package com.ddd.webbb.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ddd.webbb.user.domain.CareerLevel;
import com.ddd.webbb.user.domain.JobType;
import com.ddd.webbb.user.domain.User;
import com.ddd.webbb.user.domain.UserRepository;
import com.ddd.webbb.user.infrastructure.UserRepositoryImpl;
import com.ddd.webbb.user.interfaces.dto.UserResponse;
import com.ddd.webbb.user.interfaces.dto.UserUpdateRequest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class UserServiceTest {

    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        UserRepositoryImpl userRepositoryImpl = mock(UserRepositoryImpl.class);
        userService = new UserService(userRepository, userRepositoryImpl);
    }

    @Test
    void 회원정보수정은_직군과_경력_enum을_코드값으로_저장한다() {
        UUID userId = UUID.randomUUID();
        User user = User.createOAuthUser("ogu@test.com", "ogu", "DESIGN", "YEAR_1");
        ReflectionTestUtils.setField(user, "publicId", userId);
        given(userRepository.findByPublicIdAndDeletedAtIsNull(userId))
                .willReturn(Optional.of(user));

        UserResponse response =
                userService.updateUser(
                        userId,
                        new UserUpdateRequest("newogu", JobType.DEVELOPMENT, CareerLevel.YEAR_3));

        assertThat(response.jobType()).isEqualTo("DEVELOPMENT");
        assertThat(response.careerLevel()).isEqualTo("YEAR_3");
        assertThat(user.getJobType()).isEqualTo("DEVELOPMENT");
        assertThat(user.getCareerLevel()).isEqualTo("YEAR_3");
    }

    @Test
    void 회원정보수정에서_직군과_경력이_null이면_기존값을_유지한다() {
        UUID userId = UUID.randomUUID();
        User user = User.createOAuthUser("ogu@test.com", "ogu", "PLANNING", "YEAR_5");
        ReflectionTestUtils.setField(user, "publicId", userId);
        given(userRepository.findByPublicIdAndDeletedAtIsNull(userId))
                .willReturn(Optional.of(user));

        UserResponse response =
                userService.updateUser(userId, new UserUpdateRequest("newogu", null, null));

        assertThat(response.jobType()).isEqualTo("PLANNING");
        assertThat(response.careerLevel()).isEqualTo("YEAR_5");
        assertThat(user.getJobType()).isEqualTo("PLANNING");
        assertThat(user.getCareerLevel()).isEqualTo("YEAR_5");
    }
}
