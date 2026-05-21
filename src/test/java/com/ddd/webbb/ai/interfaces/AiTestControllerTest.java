package com.ddd.webbb.ai.interfaces;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ddd.webbb.ai.application.AiAnalysisResponse;
import com.ddd.webbb.ai.application.AiAnalysisService;
import com.ddd.webbb.ai.domain.PostContent;
import com.ddd.webbb.comment.application.CommentSummaryService;
import com.ddd.webbb.global.auth.JwtAuthFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AiTestController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AiTestControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AiAnalysisService aiAnalysisService;
    @MockitoBean private CommentSummaryService commentSummaryService;
    @MockitoBean private JwtAuthFilter jwtAuthFilter;

    @Test
    void 감정_분석_요청을_처리한다() throws Exception {
        AiAnalysisResponse mockResponse =
                new AiAnalysisResponse("ANXIETY", 30, 0.9, "불안 표현이 강함", false, "OPENAI");
        given(aiAnalysisService.analyze(any(PostContent.class))).willReturn(mockResponse);

        mockMvc.perform(
                        post("/api/ai/test/analyze")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                    {"content": "면접 때문에 너무 불안해요"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.emotionType").value("ANXIETY"))
                .andExpect(jsonPath("$.data.hp").value(30))
                .andExpect(jsonPath("$.data.crisisDetected").value(false))
                .andExpect(jsonPath("$.data.usedProvider").value("OPENAI"));
    }

    @Test
    void 빈_content는_400을_반환한다() throws Exception {
        mockMvc.perform(
                        post("/api/ai/test/analyze")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                    {"content": ""}
                    """))
                .andExpect(status().isBadRequest());
    }
}
