package com.ddd.webbb.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;

@Configuration
public class SwaggerConfig {

    private static final Map<String, ApiImplementationStatus> API_STATUS_BY_OPERATION =
            createApiStatusByOperation();

    @Bean
    public OpenAPI openAPI() {
        String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(
                        new Info()
                                .title("WEBBB API")
                                .description(
                                        """
                                        WEBBB 백엔드 API 문서

                                        라벨 안내
                                        - [LIVE]: 실제 서비스 로직과 DB/외부 연동까지 구현된 API
                                        - [STUB]: 스웨거 명세만 있고 현재는 하드코딩 또는 임시 응답인 API
                                        - [TEST]: 내부 검증/실험용으로 실제 동작하지만 프론트 연동 대상은 아닌 API

                                        프론트 연동은 [LIVE] 라벨이 붙은 API 기준으로 진행하세요.
                                        """)
                                .version("v1")
                                .contact(
                                        new Contact()
                                                .name("DDD-13 WEBBB")
                                                .url(
                                                        "https://github.com/DDD-Community/DDD-13-WEBBB_BE")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        securitySchemeName,
                                        new SecurityScheme()
                                                .name(securitySchemeName)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")));
    }

    @Bean
    public OperationCustomizer apiStatusOperationCustomizer() {
        return (operation, handlerMethod) -> {
            String operationKey = extractSingleOperationKey(handlerMethod);
            ApiImplementationStatus status = API_STATUS_BY_OPERATION.get(operationKey);
            if (status == null) {
                throw new IllegalStateException(
                        "Swagger API status is not defined for operation: " + operationKey);
            }

            String summary = operation.getSummary();
            if (summary != null && summary.startsWith("[" + status.name() + "] ")) {
                return operation;
            }
            if (summary == null || summary.isBlank()) {
                summary = operationKey;
            }
            operation.setSummary(status.prefix(summary));
            return operation;
        };
    }

    static Set<String> documentedOperationKeys() {
        return API_STATUS_BY_OPERATION.keySet();
    }

    static String extractSingleOperationKey(HandlerMethod handlerMethod) {
        return extractOperationKeys(handlerMethod.getBeanType(), handlerMethod.getMethod()).stream()
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "No operation mapping found for handler method: "
                                                + handlerMethod));
    }

    static Set<String> extractOperationKeys(Class<?> controllerClass, Method method) {
        String[] classPaths = extractPaths(controllerClass.getAnnotation(RequestMapping.class));
        String[] methodPaths = extractPathsFromMethod(method);
        HttpMethod[] methods = extractHttpMethods(method);

        Set<String> operationKeys = new LinkedHashSet<>();
        for (HttpMethod httpMethod : methods) {
            for (String classPath : classPaths) {
                for (String methodPath : methodPaths) {
                    operationKeys.add(
                            httpMethod.name()
                                    + " "
                                    + normalizePath(joinPaths(classPath, methodPath)));
                }
            }
        }
        return operationKeys;
    }

    private static Map<String, ApiImplementationStatus> createApiStatusByOperation() {
        Map<String, ApiImplementationStatus> statuses = new LinkedHashMap<>();

        put(statuses, ApiImplementationStatus.LIVE, "POST", "/api/users");
        put(statuses, ApiImplementationStatus.LIVE, "GET", "/api/users/{id}");
        put(statuses, ApiImplementationStatus.LIVE, "GET", "/api/users");
        put(statuses, ApiImplementationStatus.LIVE, "PATCH", "/api/users/{id}");
        put(statuses, ApiImplementationStatus.LIVE, "DELETE", "/api/users/{id}");
        put(statuses, ApiImplementationStatus.LIVE, "POST", "/api/posts");

        put(statuses, ApiImplementationStatus.TEST, "POST", "/api/ai/test/analyze");
        put(statuses, ApiImplementationStatus.TEST, "POST", "/api/ai/test/comment-summary");

        put(statuses, ApiImplementationStatus.LIVE, "POST", "/api/auth/oauth/{provider}");
        put(statuses, ApiImplementationStatus.LIVE, "POST", "/api/auth/oauth/exchange");
        put(statuses, ApiImplementationStatus.LIVE, "POST", "/api/auth/signup/email");
        put(statuses, ApiImplementationStatus.LIVE, "POST", "/api/auth/login/email");
        put(statuses, ApiImplementationStatus.LIVE, "POST", "/api/auth/logout");
        put(statuses, ApiImplementationStatus.LIVE, "POST", "/api/auth/refresh");
        put(statuses, ApiImplementationStatus.LIVE, "POST", "/api/auth/link/{provider}");
        put(statuses, ApiImplementationStatus.LIVE, "DELETE", "/api/auth/link/{provider}");

        put(statuses, ApiImplementationStatus.STUB, "GET", "/api/users/me");
        put(statuses, ApiImplementationStatus.STUB, "PATCH", "/api/users/me/profile");

        put(statuses, ApiImplementationStatus.LIVE, "GET", "/api/posts");
        put(statuses, ApiImplementationStatus.LIVE, "GET", "/api/posts/{postId}");
        put(statuses, ApiImplementationStatus.LIVE, "PATCH", "/api/posts/{postId}");
        put(statuses, ApiImplementationStatus.LIVE, "DELETE", "/api/posts/{postId}");

        put(statuses, ApiImplementationStatus.LIVE, "POST", "/api/posts/{postId}/comments");
        put(statuses, ApiImplementationStatus.LIVE, "GET", "/api/posts/{postId}/comments");
        put(statuses, ApiImplementationStatus.LIVE, "PATCH", "/api/comments/{commentId}");
        put(statuses, ApiImplementationStatus.LIVE, "DELETE", "/api/comments/{commentId}");

        put(
                statuses,
                ApiImplementationStatus.LIVE,
                "POST",
                "/api/posts/{postId}/comments/{commentId}/likes");
        put(
                statuses,
                ApiImplementationStatus.LIVE,
                "DELETE",
                "/api/posts/{postId}/comments/{commentId}/likes/me");

        put(statuses, ApiImplementationStatus.STUB, "POST", "/api/posts/{postId}/likes");
        put(statuses, ApiImplementationStatus.STUB, "DELETE", "/api/posts/{postId}/likes/me");

        put(statuses, ApiImplementationStatus.LIVE, "GET", "/api/me/posts");
        put(statuses, ApiImplementationStatus.LIVE, "GET", "/api/me/comments");
        put(statuses, ApiImplementationStatus.STUB, "GET", "/api/me/monster-stats");

        return Map.copyOf(statuses);
    }

    private static void put(
            Map<String, ApiImplementationStatus> statuses,
            ApiImplementationStatus status,
            String method,
            String path) {
        statuses.put(method + " " + normalizePath(path), status);
    }

    private static String[] extractPathsFromMethod(Method method) {
        List<Class<? extends Annotation>> mappingTypes =
                List.of(
                        GetMapping.class,
                        PostMapping.class,
                        PatchMapping.class,
                        DeleteMapping.class,
                        RequestMapping.class);

        return mappingTypes.stream()
                .map(type -> method.getAnnotation(type))
                .filter(annotation -> annotation != null)
                .findFirst()
                .map(SwaggerConfig::extractPaths)
                .orElse(new String[] {""});
    }

    private static HttpMethod[] extractHttpMethods(Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) {
            return new HttpMethod[] {HttpMethod.GET};
        }
        if (method.isAnnotationPresent(PostMapping.class)) {
            return new HttpMethod[] {HttpMethod.POST};
        }
        if (method.isAnnotationPresent(PatchMapping.class)) {
            return new HttpMethod[] {HttpMethod.PATCH};
        }
        if (method.isAnnotationPresent(DeleteMapping.class)) {
            return new HttpMethod[] {HttpMethod.DELETE};
        }

        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
        if (requestMapping == null || requestMapping.method().length == 0) {
            throw new IllegalStateException("HTTP method mapping is missing for: " + method);
        }

        return Arrays.stream(requestMapping.method())
                .map(requestMethod -> HttpMethod.valueOf(requestMethod.name()))
                .toArray(HttpMethod[]::new);
    }

    private static String[] extractPaths(Annotation annotation) {
        if (annotation == null) {
            return new String[] {""};
        }
        if (annotation instanceof RequestMapping requestMapping) {
            return choosePaths(requestMapping.path(), requestMapping.value());
        }
        if (annotation instanceof GetMapping getMapping) {
            return choosePaths(getMapping.path(), getMapping.value());
        }
        if (annotation instanceof PostMapping postMapping) {
            return choosePaths(postMapping.path(), postMapping.value());
        }
        if (annotation instanceof PatchMapping patchMapping) {
            return choosePaths(patchMapping.path(), patchMapping.value());
        }
        if (annotation instanceof DeleteMapping deleteMapping) {
            return choosePaths(deleteMapping.path(), deleteMapping.value());
        }
        return new String[] {""};
    }

    private static String[] choosePaths(String[] paths, String[] values) {
        String[] candidates = paths.length > 0 ? paths : values;
        return candidates.length > 0 ? candidates : new String[] {""};
    }

    private static String joinPaths(String basePath, String methodPath) {
        if (basePath == null || basePath.isBlank()) {
            return methodPath == null || methodPath.isBlank() ? "/" : methodPath;
        }
        if (methodPath == null || methodPath.isBlank()) {
            return basePath;
        }
        return basePath + "/" + methodPath;
    }

    private static String normalizePath(String path) {
        String normalized =
                Stream.of(path.split("/"))
                        .filter(segment -> !segment.isBlank())
                        .reduce("", (acc, segment) -> acc + "/" + segment);
        return normalized.isEmpty() ? "/" : normalized;
    }

    enum ApiImplementationStatus {
        LIVE,
        STUB,
        TEST;

        String prefix(String summary) {
            return "[" + name() + "] " + summary;
        }
    }
}
