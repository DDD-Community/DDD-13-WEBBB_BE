package com.ddd.webbb.ai.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ddd.webbb.ai.application.AiAnalysisService;
import com.ddd.webbb.comment.application.CommentSummaryService;
import com.ddd.webbb.global.auth.JwtAuthFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AiTestController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("prod")
@TestPropertySource(properties = "LOG_PATH=build/test-logs")
class AiTestControllerProdProfileTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AiAnalysisService aiAnalysisService;
    @MockitoBean private CommentSummaryService commentSummaryService;
    @MockitoBean private JwtAuthFilter jwtAuthFilter;

    @Test
    void prod_프로필에서는_ai_test_api를_사용할_수_없다() throws Exception {
        mockMvc.perform(
                        post("/api/ai/test/analyze")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                    {"content": "프로덕션에서도 테스트하고 싶어요"}
                    """))
                .andExpect(status().isNotFound());
    }
}
