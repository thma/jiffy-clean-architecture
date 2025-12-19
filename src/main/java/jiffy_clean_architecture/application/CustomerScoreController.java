package jiffy_clean_architecture.application;

import jiffy_clean_architecture.usecases.CustomerScoreUseCase;
import org.jiffy.core.Eff;
import org.jiffy.core.EffectRuntime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller that uses the effect-based customer score use case.
 * This demonstrates how to integrate algebraic effects with Spring Boot.
 */
@RestController
@RequestMapping("/customers")
public class CustomerScoreController {

    private final CustomerScoreUseCase useCase;
    private final EffectRuntime runtime;

    public CustomerScoreController(
        CustomerScoreUseCase useCase,
        EffectRuntime runtime
    ) {
        this.useCase = useCase;
        this.runtime = runtime;
    }

    /**
     * Calculate customer score using algebraic effects.
     *
     * @param id the customer ID
     * @return the calculated score
     */
    @GetMapping("/{id}/score")
    public ResponseEntity<ScoreResponse> getScore(@PathVariable Long id) {
        try {
            // Create the effect computation
            Eff<Integer> scoreEffect = useCase.calculateScore(id);

            // Run the effect with the runtime
            Integer score = scoreEffect.runWith(runtime);

            return ResponseEntity.ok(new ScoreResponse(id, score, "Success"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ScoreResponse(id, 0, "Error: " + e.getMessage()));
        }
    }

    /**
     * Calculate customer score with error recovery.
     */
    @GetMapping("/{id}/score-safe")
    public ResponseEntity<ScoreResponse> getScoreSafe(@PathVariable Long id) {
        try {
            // Create the effect computation with recovery
            Eff<Integer> scoreEffect = useCase.calculateScoreWithRecovery(id);

            // Run the effect with the runtime
            Integer score = scoreEffect.runWith(runtime);

            return ResponseEntity.ok(new ScoreResponse(id, score, "Success"));
        } catch (Exception e) {
            // This should rarely happen due to recovery
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ScoreResponse(id, 0, "Unexpected error: " + e.getMessage()));
        }
    }

    /**
     * Calculate customer score using sequential fetching.
     */
    @GetMapping("/{id}/score-sequential")
    public ResponseEntity<ScoreResponse> getScoreSequential(@PathVariable Long id) {
        try {
            // Create the effect computation
            Eff<Integer> scoreEffect = useCase.calculateScore(id);

            // Run the effect with the runtime
            Integer score = scoreEffect.runWith(runtime);

            return ResponseEntity.ok(new ScoreResponse(id, score, "Success"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ScoreResponse(id, 0, "Error: " + e.getMessage()));
        }
    }

    /**
     * Response DTO for score endpoints.
     */
    public static class ScoreResponse {
        private final Long customerId;
        private final Integer score;
        private final String status;

        public ScoreResponse(Long customerId, Integer score, String status) {
            this.customerId = customerId;
            this.score = score;
            this.status = status;
        }

        public Long getCustomerId() {
            return customerId;
        }

        public Integer getScore() {
            return score;
        }

        public String getStatus() {
            return status;
        }
    }
}