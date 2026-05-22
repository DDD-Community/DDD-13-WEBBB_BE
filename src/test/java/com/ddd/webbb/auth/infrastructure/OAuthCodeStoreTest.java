package com.ddd.webbb.auth.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ddd.webbb.global.common.exception.AppException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class OAuthCodeStoreTest {

    @Mock private StringRedisTemplate redisTemplate;

    @Mock private ValueOperations<String, String> valueOperations;

    private OAuthCodeStore oAuthCodeStore;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        oAuthCodeStore = new OAuthCodeStore(redisTemplate, new ObjectMapper());
    }

    @Test
    @DisplayName("save → exchange: 유효한 코드로 토큰 쌍 반환")
    void exchange_validCode_returnsTokens() {
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        String code = oAuthCodeStore.save("at-123", "rt-456");
        assertNotNull(code);

        verify(valueOperations)
                .set(keyCaptor.capture(), valueCaptor.capture(), eq(30L), eq(TimeUnit.SECONDS));

        String savedKey = keyCaptor.getValue();
        String savedJson = valueCaptor.getValue();

        when(valueOperations.getAndDelete(savedKey)).thenReturn(savedJson);

        OAuthCodeStore.TokenPair result = oAuthCodeStore.exchange(code);
        assertEquals("at-123", result.accessToken());
        assertEquals("rt-456", result.refreshToken());
    }

    @Test
    @DisplayName("같은 코드 2회 교환 시도 → 두 번째에서 실패 (1회성 보장)")
    void exchange_sameCodeTwice_secondCallFails() {
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        String code = oAuthCodeStore.save("at-123", "rt-456");

        verify(valueOperations)
                .set(keyCaptor.capture(), valueCaptor.capture(), eq(30L), eq(TimeUnit.SECONDS));

        String savedKey = keyCaptor.getValue();
        String savedJson = valueCaptor.getValue();

        // 첫 번째 교환: 성공 (getAndDelete가 값을 반환하고 삭제)
        when(valueOperations.getAndDelete(savedKey)).thenReturn(savedJson).thenReturn(null);

        OAuthCodeStore.TokenPair result = oAuthCodeStore.exchange(code);
        assertEquals("at-123", result.accessToken());

        // 두 번째 교환: getAndDelete가 null 반환 → 실패
        assertThrows(AppException.class, () -> oAuthCodeStore.exchange(code));
    }

    @Test
    @DisplayName("존재하지 않는 코드 교환 → 실패")
    void exchange_unknownCode_throwsException() {
        when(valueOperations.getAndDelete(anyString())).thenReturn(null);

        assertThrows(AppException.class, () -> oAuthCodeStore.exchange("nonexistent-code"));
    }
}
