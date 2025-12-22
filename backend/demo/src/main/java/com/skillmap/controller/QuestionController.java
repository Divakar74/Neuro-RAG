package com.skillmap.controller;

import com.skillmap.model.entity.AssessmentSession;
import com.skillmap.model.entity.Question;
import com.skillmap.model.entity.Response;
import com.skillmap.model.dto.UserSessionQuestionDto;
import com.skillmap.repository.AssessmentSessionRepository;
import com.skillmap.repository.QuestionRepository;
import com.skillmap.repository.ResponseRepository;
import com.skillmap.service.engine.AdaptiveQuestionEngine;
import com.skillmap.service.engine.StoppingCriteriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionRepository questionRepository;
    private final AssessmentSessionRepository sessionRepository;
    private final ResponseRepository responseRepository;
    private final AdaptiveQuestionEngine adaptiveQuestionEngine;
    private final StoppingCriteriaService stoppingCriteriaService;

    @GetMapping
    public ResponseEntity<List<Question>> getAllQuestions() {
        List<Question> questions = questionRepository.findAll();
        // Refresh cache after fetching all questions (in case questions were updated)
        adaptiveQuestionEngine.refreshQuestionsCache();
        return ResponseEntity.ok(questions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Question> getQuestionById(@PathVariable Long id) {
        Optional<Question> questionOpt = questionRepository.findById(id);
        if (questionOpt.isPresent()) {
            return ResponseEntity.ok(questionOpt.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/skill/{skillId}")
    public ResponseEntity<List<Question>> getQuestionsBySkill(@PathVariable Long skillId) {
        List<Question> questions = questionRepository.findBySkillId(skillId);
        return ResponseEntity.ok(questions);
    }

    @PostMapping
    public ResponseEntity<Question> createQuestion(@RequestBody Question question) {
        Question savedQuestion = questionRepository.save(question);
        // Refresh cache after creating a new question
        adaptiveQuestionEngine.refreshQuestionsCache();
        return ResponseEntity.ok(savedQuestion);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Question> updateQuestion(@PathVariable Long id, @RequestBody Question questionDetails) {
        Optional<Question> questionOpt = questionRepository.findById(id);
        if (questionOpt.isPresent()) {
            Question question = questionOpt.get();
            question.setQuestionText(questionDetails.getQuestionText());
            question.setDifficultyLevel(questionDetails.getDifficultyLevel());
            question.setExpectedKeywords(questionDetails.getExpectedKeywords());
            question.setLevelIndicators(questionDetails.getLevelIndicators());
            question.setSuggestedAnswerLength(questionDetails.getSuggestedAnswerLength());
            question.setContextHint(questionDetails.getContextHint());
            question.setFollowUpText(questionDetails.getFollowUpText());
            question.setTimesAsked(questionDetails.getTimesAsked());
            question.setAvgResponseTime(questionDetails.getAvgResponseTime());
            Question updatedQuestion = questionRepository.save(question);
            // Refresh cache after updating a question
            adaptiveQuestionEngine.refreshQuestionsCache();
            return ResponseEntity.ok(updatedQuestion);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        if (questionRepository.existsById(id)) {
            questionRepository.deleteById(id);
            // Refresh cache after deleting a question
            adaptiveQuestionEngine.refreshQuestionsCache();
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Adaptive Question Engine Endpoints

    @GetMapping("/next/{sessionToken}")
    public ResponseEntity<?> getNextQuestion(@PathVariable String sessionToken) {
        try {
            Optional<AssessmentSession> sessionOpt = sessionRepository.findBySessionToken(sessionToken);
            if (!sessionOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            AssessmentSession session = sessionOpt.get();

            // Check if assessment should stop
            if (stoppingCriteriaService.shouldStopAssessment(session)) {
                Map<String, Object> stopResponse = Map.of(
                    "shouldStop", true,
                    "reason", stoppingCriteriaService.getStopReason(session),
                    "status", stoppingCriteriaService.getStoppingCriteriaStatus(session)
                );
                return ResponseEntity.ok(stopResponse);
            }

            Question nextQuestion = adaptiveQuestionEngine.selectNextQuestion(session);
            if (nextQuestion == null) {
                Map<String, Object> stopResponse = Map.of(
                    "shouldStop", true,
                    "reason", "NO_MORE_QUESTIONS",
                    "status", stoppingCriteriaService.getStoppingCriteriaStatus(session)
                );
                return ResponseEntity.ok(stopResponse);
            }

            return ResponseEntity.ok(nextQuestion);
        } catch (Exception e) {
            // Log the error for debugging
            System.err.println("Error fetching next question for session " + sessionToken + ": " + e.getMessage());
            e.printStackTrace();

            // Return a meaningful error response instead of 500
            Map<String, Object> errorResponse = Map.of(
                "error", "INTERNAL_SERVER_ERROR",
                "message", "An error occurred while fetching the next question. Please try again later.",
                "sessionToken", sessionToken
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/progress/{sessionToken}")
    public ResponseEntity<Map<String, Object>> getAssessmentProgress(@PathVariable String sessionToken) {
        Optional<AssessmentSession> sessionOpt = sessionRepository.findBySessionToken(sessionToken);
        if (!sessionOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        AssessmentSession session = sessionOpt.get();
        Map<String, Object> progress = adaptiveQuestionEngine.getAssessmentProgress(session);

        return ResponseEntity.ok(progress);
    }

    @GetMapping("/recommended/{sessionToken}")
    public ResponseEntity<List<Question>> getRecommendedQuestions(
            @PathVariable String sessionToken,
            @RequestParam(defaultValue = "5") int count) {
        Optional<AssessmentSession> sessionOpt = sessionRepository.findBySessionToken(sessionToken);
        if (!sessionOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        AssessmentSession session = sessionOpt.get();
        List<Question> recommended = adaptiveQuestionEngine.getRecommendedQuestions(session, count);

        return ResponseEntity.ok(recommended);
    }

    @GetMapping("/stopping-criteria/{sessionToken}")
    public ResponseEntity<Map<String, Object>> getStoppingCriteriaStatus(@PathVariable String sessionToken) {
        Optional<AssessmentSession> sessionOpt = sessionRepository.findBySessionToken(sessionToken);
        if (!sessionOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        AssessmentSession session = sessionOpt.get();
        Map<String, Object> status = stoppingCriteriaService.getStoppingCriteriaStatus(session);

        return ResponseEntity.ok(status);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserSessionQuestionDto>> getUserSessionQuestions(@PathVariable Long userId) {
        // Get all sessions for the user
        List<AssessmentSession> userSessions = sessionRepository.findByUserId(userId);
        if (userSessions.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        // Get all responses for the user
        List<Response> userResponses = responseRepository.findByUserId(userId);

        // Create a map of questionId -> response for quick lookup
        Map<Long, Response> responseMap = userResponses.stream()
            .collect(Collectors.toMap(r -> r.getQuestion().getId(), r -> r));

        // Build the result list
        List<UserSessionQuestionDto> result = userSessions.stream()
            .flatMap(session -> {
                // Get all responses for this session
                List<Response> sessionResponses = userResponses.stream()
                    .filter(r -> r.getSession().getId().equals(session.getId()))
                    .toList();

                // Create DTOs for each question-response pair in this session
                return sessionResponses.stream()
                    .map(response -> {
                        Question question = response.getQuestion();
                        UserSessionQuestionDto dto = new UserSessionQuestionDto();

                        // Session info
                        dto.setSessionId(session.getId());
                        dto.setSessionToken(session.getSessionToken());
                        dto.setSessionStartedAt(session.getStartedAt());
                        dto.setTargetRole(session.getTargetRole());

                        // Question info
                        dto.setQuestionId(question.getId());
                        dto.setQuestionText(question.getQuestionText());
                        dto.setQuestionType(question.getQuestionType());
                        dto.setDifficulty(question.getDifficulty());
                        dto.setTopic(question.getTopic());
                        dto.setOptions(question.getOptions());
                        dto.setCorrectAnswer(question.getCorrectAnswer());
                        dto.setExplanation(question.getExplanation());

                        // Response info
                        dto.setResponseId(response.getId());
                        dto.setResponseText(response.getResponseText());
                        dto.setWordCount(response.getWordCount());
                        dto.setCharCount(response.getCharCount());
                        dto.setTotalTimeSeconds(response.getTotalTimeSeconds());
                        dto.setThinkTimeSeconds(response.getThinkTimeSeconds());
                        dto.setEditCount(response.getEditCount());
                        dto.setPasteDetected(response.getPasteDetected());
                        dto.setConfidenceLevel(response.getConfidenceLevel());
                        dto.setIsCorrect(response.getIsCorrect());
                        dto.setTypingSpeedWpm(response.getTypingSpeedWpm());
                        dto.setSimilarityScore(response.getSimilarityScore());
                        dto.setSpecificityScore(response.getSpecificityScore());
                        dto.setDepthScore(response.getDepthScore());
                        dto.setAnsweredAt(response.getAnsweredAt());

                        return dto;
                    });
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
