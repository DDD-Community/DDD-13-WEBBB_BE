package com.ddd.webbb.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.LongSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class MdcFilterTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(MdcFilter.class);
    private final Level originalLevel = logger.getLevel();
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        logger.setLevel(originalLevel);
        appender.stop();
    }

    @Test
    void 요청_시작과_종료_로그를_info로_남기고_민감_쿼리를_마스킹한다() throws Exception {
        MdcFilter filter = new MdcFilter(supplierOf(1_000L, 1_050L), 1_000L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        request.setQueryString("email=test@example.com&page=1");
        request.addHeader("X-Request-Id", "trace-1234");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        attachAppender();

        filter.doFilterInternal(request, response, noopChain());

        assertThat(response.getHeader("X-Request-Id")).isEqualTo("trace-1234");
        assertThat(appender.list).hasSize(2);
        assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.INFO);
        assertThat(appender.list.get(0).getFormattedMessage())
                .isEqualTo(
                        "HTTP IN method=GET path=/api/users query=?email=***&page=1 clientIp=203.0.113.10");
        assertThat(appender.list.get(1).getLevel()).isEqualTo(Level.INFO);
        assertThat(appender.list.get(1).getFormattedMessage())
                .isEqualTo("HTTP OUT method=GET path=/api/users status=200 durationMs=50");
        assertThat(appender.list.get(1).getMDCPropertyMap())
                .containsEntry("requestId", "trace-1234");
    }

    @Test
    void 잘못된_request_id는_새로_발급한다() throws Exception {
        MdcFilter filter = new MdcFilter(supplierOf(1_000L, 1_010L), 1_000L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        request.addHeader("X-Request-Id", "invalid request id");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        attachAppender();

        filter.doFilterInternal(request, response, noopChain());

        String issuedRequestId = response.getHeader("X-Request-Id");
        assertThat(issuedRequestId).matches("^[a-zA-Z0-9\\-]{1,64}$");
        assertThat(issuedRequestId).isNotEqualTo("invalid request id");
        assertThat(appender.list.get(1).getMDCPropertyMap())
                .containsEntry("requestId", issuedRequestId);
    }

    @Test
    void 서버_오류는_warn으로_남긴다() throws Exception {
        MdcFilter filter = new MdcFilter(supplierOf(1_000L, 1_050L), 1_000L);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        attachAppender();

        filter.doFilterInternal(
                request, response, (req, res) -> ((MockHttpServletResponse) res).setStatus(500));

        assertThat(appender.list).hasSize(2);
        assertThat(appender.list.get(1).getLevel()).isEqualTo(Level.WARN);
        assertThat(appender.list.get(1).getFormattedMessage())
                .isEqualTo("HTTP OUT method=POST path=/api/users status=500 durationMs=50");
    }

    @Test
    void 지연_요청은_warn으로_남긴다() throws Exception {
        MdcFilter filter = new MdcFilter(supplierOf(1_000L, 2_500L), 1_000L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        attachAppender();

        filter.doFilterInternal(request, response, noopChain());

        assertThat(appender.list).hasSize(2);
        assertThat(appender.list.get(1).getLevel()).isEqualTo(Level.WARN);
        assertThat(appender.list.get(1).getFormattedMessage())
                .isEqualTo("HTTP OUT method=GET path=/api/users status=200 durationMs=1500");
    }

    @Test
    void 제외_경로는_필터에서_제외한다() {
        MdcFilter filter = new MdcFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    private void attachAppender() {
        logger.setLevel(Level.INFO);
        appender.start();
        logger.addAppender(appender);
    }

    private FilterChain noopChain() {
        return (request, response) -> {};
    }

    private LongSupplier supplierOf(long... values) {
        Queue<Long> queue = new ArrayDeque<>();
        for (long value : values) {
            queue.add(value);
        }
        return () -> {
            Long value = queue.poll();
            if (value == null) {
                throw new IllegalStateException("No more timestamps available");
            }
            return value;
        };
    }
}
