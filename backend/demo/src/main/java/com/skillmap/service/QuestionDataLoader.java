
package com.skillmap.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillmap.model.entity.Question;
import com.skillmap.model.entity.Skill;
import com.skillmap.repository.QuestionRepository;
import com.skillmap.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionDataLoader implements CommandLineRunner {

    private final QuestionRepository questionRepository;
    private final SkillRepository skillRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) throws Exception {
        loadBehavioralQuestions();
        loadSystemDesignQuestions();
        loadTechnicalQuestions();
    }

    private void loadBehavioralQuestions() {
        try {
            // Use external dataset path
            java.io.File file = new java.io.File("D:/Neuro-RAG-app/dataset/behavioral_prompts.json");
            if (!file.exists()) {
                log.warn("Behavioral questions file not found: D:/Neuro-RAG-app/dataset/behavioral_prompts.json");
                return;
            }

            List<Map<String, Object>> questions = objectMapper.readValue(file,
                new TypeReference<List<Map<String, Object>>>() {});

            for (Map<String, Object> q : questions) {
                saveQuestion(q, "behavioral", "Behavioral");
            }

            log.info("Loaded {} behavioral questions", questions.size());
        } catch (IOException e) {
            log.error("Failed to load behavioral questions", e);
        }
    }

    private void loadSystemDesignQuestions() {
        try {
            // Use external dataset path
            java.io.File file = new java.io.File("D:/Neuro-RAG-app/dataset/system_design_mcqs.json");
            if (!file.exists()) {
                log.warn("System design questions file not found: D:/Neuro-RAG-app/dataset/system_design_mcqs.json");
                return;
            }

            List<Map<String, Object>> questions = objectMapper.readValue(file,
                new TypeReference<List<Map<String, Object>>>() {});

            for (Map<String, Object> q : questions) {
                saveQuestion(q, "system_design", "System Design");
            }

            log.info("Loaded {} system design questions", questions.size());
        } catch (IOException e) {
            log.error("Failed to load system design questions", e);
        }
    }

    private void loadTechnicalQuestions() {
        try {
            // Use external dataset path
            java.io.File file = new java.io.File("D:/Neuro-RAG-app/dataset/technical_mcqs.json");
            if (!file.exists()) {
                log.warn("Technical questions file not found: D:/Neuro-RAG-app/dataset/technical_mcqs.json");
                return;
            }

            List<Map<String, Object>> questions = objectMapper.readValue(file,
                new TypeReference<List<Map<String, Object>>>() {});

            for (Map<String, Object> q : questions) {
                saveQuestion(q, "technical", "Technical");
            }

            log.info("Loaded {} technical questions", questions.size());
        } catch (IOException e) {
            log.error("Failed to load technical questions", e);
        }
    }

    private void saveQuestion(Map<String, Object> questionData, String skillName, String topic) {
        try {
            String questionText = (String) questionData.get("question");
            if (questionText == null || questionText.trim().isEmpty()) {
                questionText = (String) questionData.get("text");
            }
            if (questionText == null || questionText.trim().isEmpty()) {
                return; // skip invalid entries
            }

            Optional<Question> existingQuestion = questionRepository.findByQuestionText(questionText);

            Optional<Skill> skillOpt = skillRepository.findBySkillCode(skillName);
            Skill skill = skillOpt.orElseGet(() -> {
                Skill newSkill = new Skill();
                newSkill.setSkillCode(skillName);
                newSkill.setDisplayName(skillName);
                newSkill.setDescription(skillName + " assessment");
                newSkill.setCategory(Skill.Category.Programming);
                return skillRepository.save(newSkill);
            });

            Question question = existingQuestion.orElse(new Question());
            question.setSkill(skill);
            question.setQuestionText(questionText);

            Object diff = questionData.getOrDefault("difficulty", questionData.get("level"));
            question.setDifficulty(diff != null ? diff.toString() : "Easy");
            question.setTopic(topic);

            // Options mapping: options | choices | answers array
            Object optionsRaw = questionData.get("options");
            if (optionsRaw == null) optionsRaw = questionData.get("choices");
            if (optionsRaw == null) optionsRaw = questionData.get("answers");

            if (optionsRaw instanceof List) {
                @SuppressWarnings("unchecked") List<Object> list = (List<Object>) optionsRaw;
                // If objects, map to display strings
                List<String> normalized = list.stream()
                    .map(o -> {
                        if (o == null) return "";
                        if (o instanceof String) return (String) o;
                        if (o instanceof Map) {
                            Map<?,?> m = (Map<?,?>) o;
                            Object text = m.get("text");
                            if (text == null) text = m.get("label");
                            if (text == null) text = m.get("option");
                            if (text == null) text = m.get("value");
                            if (text == null && !m.isEmpty()) text = m.values().iterator().next();
                            return text != null ? text.toString() : "";
                        }
                        return o.toString();
                    })
                    .toList();
                question.setOptions(objectMapper.writeValueAsString(normalized));
                question.setQuestionType("mcq");
            } else {
                question.setQuestionType("text");
            }

            // Correct answer mapping
            String correctAnswer = null;
            Object ans = questionData.get("answer");
            if (ans == null) ans = questionData.get("correct_answer");
            if (ans == null) ans = questionData.get("correctAnswer");
            if (ans != null) correctAnswer = ans.toString();
            question.setCorrectAnswer(correctAnswer);

            // Explanation mapping
            String explanation = (String) questionData.get("explanation");
            question.setExplanation(explanation);

            questionRepository.save(question);
        } catch (Exception e) {
            log.error("Failed to save question: {}", questionData.get("question"), e);
        }
    }
}
