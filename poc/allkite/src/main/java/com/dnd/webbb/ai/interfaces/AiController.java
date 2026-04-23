package com.dnd.webbb.ai.interfaces;

import com.dnd.webbb.ai.application.AiEncouragementService;
import com.dnd.webbb.ai.application.dto.AnalysisResponse;
import com.dnd.webbb.ai.interfaces.dto.AnalyzeRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiEncouragementService aiEncouragementService;

    public AiController(AiEncouragementService aiEncouragementService) {
        this.aiEncouragementService = aiEncouragementService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResponse> analyze(@Valid @RequestBody AnalyzeRequest request) {
        AnalysisResponse result = aiEncouragementService.analyze(request.text(), request.tone());
        return ResponseEntity.ok(result);
    }
}
