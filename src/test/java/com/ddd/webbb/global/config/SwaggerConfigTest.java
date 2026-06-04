package com.ddd.webbb.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.ddd.webbb.ai.interfaces.AiTestController;
import com.ddd.webbb.auth.interfaces.AuthController;
import com.ddd.webbb.auth.interfaces.dto.EmailLoginRequest;
import com.ddd.webbb.comment.interfaces.CommentController;
import com.ddd.webbb.comment.interfaces.CommentLikeController;
import com.ddd.webbb.mypage.interfaces.MyPageController;
import com.ddd.webbb.post.interfaces.LikeController;
import com.ddd.webbb.post.interfaces.PostController;
import com.ddd.webbb.user.interfaces.UserController;
import com.ddd.webbb.user.interfaces.dto.UserCreateRequest;
import io.swagger.v3.oas.models.Operation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.web.method.HandlerMethod;

class SwaggerConfigTest {

    private final SwaggerConfig swaggerConfig = new SwaggerConfig();

    @Test
    void 실제_구현_api는_live_prefix를_붙인다() throws Exception {
        OperationCustomizer customizer = swaggerConfig.apiStatusOperationCustomizer();
        HandlerMethod handlerMethod =
                new HandlerMethod(
                        new UserController(null),
                        UserController.class.getMethod("createUser", UserCreateRequest.class));
        Operation operation = new Operation().summary("회원 생성");

        Operation customized = customizer.customize(operation, handlerMethod);

        assertThat(customized.getSummary()).isEqualTo("[LIVE] 회원 생성");
    }

    @Test
    void 스텁_api는_stub_prefix를_붙인다() throws Exception {
        OperationCustomizer customizer = swaggerConfig.apiStatusOperationCustomizer();
        HandlerMethod handlerMethod =
                new HandlerMethod(
                        new UserController(null), UserController.class.getMethod("getMe"));
        Operation operation = new Operation().summary("내 정보 조회");

        Operation customized = customizer.customize(operation, handlerMethod);

        assertThat(customized.getSummary()).isEqualTo("[STUB] 내 정보 조회");
    }

    @Test
    void 테스트_api는_test_prefix를_붙인다() throws Exception {
        OperationCustomizer customizer = swaggerConfig.apiStatusOperationCustomizer();
        HandlerMethod handlerMethod =
                new HandlerMethod(
                        new AiTestController(null, null),
                        AiTestController.class.getMethod(
                                "analyze",
                                com.ddd.webbb.ai.interfaces.dto.AiAnalyzeTestRequest.class));
        Operation operation = new Operation().summary("감정 분석 테스트");

        Operation customized = customizer.customize(operation, handlerMethod);

        assertThat(customized.getSummary()).isEqualTo("[TEST] 감정 분석 테스트");
    }

    @Test
    void 인증_실구현_api는_live_prefix를_붙인다() throws Exception {
        OperationCustomizer customizer = swaggerConfig.apiStatusOperationCustomizer();
        HandlerMethod handlerMethod =
                new HandlerMethod(
                        new AuthController(null, null),
                        AuthController.class.getMethod("loginEmail", EmailLoginRequest.class));
        Operation operation = new Operation().summary("이메일 로그인");

        Operation customized = customizer.customize(operation, handlerMethod);

        assertThat(customized.getSummary()).isEqualTo("[LIVE] 이메일 로그인");
    }

    @Test
    void 문서화된_모든_컨트롤러_operation은_상태가_정의되어_있다() {
        Set<String> controllerOperationKeys = new LinkedHashSet<>();

        for (Class<?> controllerClass : documentedControllers()) {
            for (Method method : controllerClass.getDeclaredMethods()) {
                if (hasMappingAnnotation(method)) {
                    controllerOperationKeys.addAll(
                            SwaggerConfig.extractOperationKeys(controllerClass, method));
                }
            }
        }

        assertThat(SwaggerConfig.documentedOperationKeys())
                .containsExactlyInAnyOrderElementsOf(controllerOperationKeys);
    }

    private static List<Class<?>> documentedControllers() {
        return List.of(
                AiTestController.class,
                AuthController.class,
                CommentController.class,
                CommentLikeController.class,
                LikeController.class,
                MyPageController.class,
                PostController.class,
                UserController.class);
    }

    private static boolean hasMappingAnnotation(Method method) {
        return Arrays.stream(method.getAnnotations())
                .map(annotation -> annotation.annotationType().getSimpleName())
                .anyMatch(name -> name.endsWith("Mapping"));
    }
}
