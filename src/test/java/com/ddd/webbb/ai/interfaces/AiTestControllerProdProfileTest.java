package com.ddd.webbb.ai.interfaces;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ddd.webbb.ai.application.AiAnalysisResponse;
import com.ddd.webbb.ai.application.AiAnalysisService;
import com.ddd.webbb.ai.domain.PostContent;
import com.ddd.webbb.global.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AiTestController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("prod")
@TestPropertySource(properties = "LOG_PATH=build/test-logs")
class AiTestControllerProdProfileTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiAnalysisService aiAnalysisService;

    @Test
    void prod_프로필에서도_감정_분석_요청을_처리한다() throws Exception {
        AiAnalysisResponse mockResponse = new AiAnalysisResponse(
            "ANXIETY", 30, 0.9, "불안 표현이 강함", false, "OPENAI");
        given(aiAnalysisService.analyze(any(PostContent.class))).willReturn(mockResponse);

        mockMvc.perform(post("/api/ai/test/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"content": "프로덕션에서도 테스트하고 싶어요"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.usedProvider").value("OPENAI"));
    }
}
