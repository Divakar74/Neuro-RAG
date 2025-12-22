package com.skillmap.service.analysis;

import com.skillmap.model.entity.Response;
import com.skillmap.model.entity.Skill;
import com.skillmap.model.entity.SkillDependency;
import com.skillmap.model.entity.Resource;
import com.skillmap.repository.SkillRepository;
import com.skillmap.repository.SkillDependencyRepository;
import com.skillmap.repository.ResourceRepository;
import com.skillmap.service.OpenAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RAGService {

    private final SkillRepository skillRepository;
    private final SkillDependencyRepository skillDependencyRepository;
    private final ResourceRepository resourceRepository;
    private final OpenAIService openAIService;

    /**
     * Generates personalized suggestions using Retrieval-Augmented Generation (RAG)
     * with a symbolic layer based on system knowledge to reduce hallucination.
     *
     * @param sessionId The assessment session ID
     * @param responses The user's responses
     * @return AI-generated suggestions grounded in system knowledge
     */
    public String generateSuggestionsWithRAG(Long sessionId, List<Response> responses) {
        log.info("Generating RAG-based suggestions for session: {}", sessionId);

        // Step 1: Analyze user responses to identify relevant skills
        List<String> relevantSkillNames = extractRelevantSkills(responses);

        // Step 2: Retrieve symbolic knowledge from the database
        String retrievedKnowledge = retrieveKnowledge(relevantSkillNames);

        // Step 3: Create augmented prompt with retrieved knowledge
        String augmentedPrompt = createAugmentedPrompt(responses, retrievedKnowledge);

        // Step 4: Generate suggestions using OpenAI with retrieved context
        String suggestions = openAIService.generateSuggestion(augmentedPrompt);

        log.info("Generated RAG-based suggestions for session: {}", sessionId);
        return suggestions;
    }

    /**
     * Generates personalized suggestions using Retrieval-Augmented Generation (RAG)
     * with cognitive bias analysis for Neuro-RAG architecture.
     *
     * @param sessionId The assessment session ID
     * @param responses The user's responses
     * @param biases The cognitive biases identified
     * @return AI-generated suggestions incorporating cognitive context
     */
    public String generateSuggestionsWithRAGAndBiases(Long sessionId, List<Response> responses,
                                                      List<CognitiveBiasAnalysisService.CognitiveBiasResult> biases) {
        log.info("Generating Neuro-RAG suggestions with cognitive context for session: {}", sessionId);

        // Step 1: Analyze user responses to identify relevant skills
        List<String> relevantSkillNames = extractRelevantSkills(responses);

        // Step 2: Retrieve symbolic knowledge from the database
        String retrievedKnowledge = retrieveKnowledge(relevantSkillNames);

        // Step 3: Create augmented prompt with retrieved knowledge and cognitive biases
        String augmentedPrompt = createAugmentedPromptWithBiases(responses, retrievedKnowledge, biases);

        // Step 4: Generate suggestions using OpenAI with cognitive context
        String suggestions = openAIService.generateSuggestion(augmentedPrompt);

        log.info("Generated Neuro-RAG suggestions for session: {}", sessionId);
        return suggestions;
    }

    /**
     * Extracts relevant skill names from user responses
     */
    private List<String> extractRelevantSkills(List<Response> responses) {
        // Simple keyword-based extraction (can be enhanced with NLP)
        return responses.stream()
            .flatMap(response -> {
                String text = response.getResponseText().toLowerCase();
                // Extract potential skill mentions (this is a basic implementation)
                return skillRepository.findAll().stream()
                    .filter(skill -> text.contains(skill.getDisplayName().toLowerCase()))
                    .map(Skill::getDisplayName);
            })
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * Retrieves relevant knowledge from the symbolic knowledge base
     */
    private String retrieveKnowledge(List<String> relevantSkillNames) {
        StringBuilder knowledge = new StringBuilder();

        // Retrieve skills information
        knowledge.append("SKILLS KNOWLEDGE:\n");
        for (String skillName : relevantSkillNames) {
            skillRepository.findByDisplayName(skillName).ifPresent(skill -> {
                knowledge.append(String.format("- %s: %s (Category: %s)\n",
                    skill.getDisplayName(),
                    skill.getDescription() != null ? skill.getDescription() : "No description",
                    skill.getCategory() != null ? skill.getCategory() : "Unknown"));
            });
        }

        // Retrieve skill dependencies
        knowledge.append("\nSKILL DEPENDENCIES:\n");
        for (String skillName : relevantSkillNames) {
            skillRepository.findByDisplayName(skillName).ifPresent(skill -> {
                List<SkillDependency> dependencies = skillDependencyRepository.findByParentSkillId(skill.getId());
                if (!dependencies.isEmpty()) {
                    knowledge.append(String.format("Dependencies for %s:\n", skillName));
                    for (SkillDependency dep : dependencies) {
                        knowledge.append(String.format("  - Requires: %s (Weight: %.2f, Type: %s)\n",
                            dep.getChildSkill().getDisplayName(), dep.getWeight(), dep.getDependencyType()));
                    }
                }
            });
        }

        // Retrieve learning resources
        knowledge.append("\nLEARNING RESOURCES:\n");
        for (String skillName : relevantSkillNames) {
            skillRepository.findByDisplayName(skillName).ifPresent(skill -> {
                List<Resource> resources = resourceRepository.findBySkillId(skill.getId());
                if (!resources.isEmpty()) {
                    knowledge.append(String.format("Resources for %s:\n", skillName));
                    for (Resource resource : resources) {
                        knowledge.append(String.format("  - %s (%s): %s\n",
                            resource.getTitle(),
                            resource.getResourceType(),
                            resource.getUrl() != null ? resource.getUrl() : resource.getDescription()));
                    }
                }
            });
        }

        return knowledge.toString();
    }

    /**
     * Creates an augmented prompt combining user responses with retrieved knowledge
     */
    private String createAugmentedPrompt(List<Response> responses, String retrievedKnowledge) {
        String responsesSummary = responses.stream()
            .map(response -> String.format(
                "Question: %s\nResponse: %s\nSpecificity Score: %.2f\nDepth Score: %.2f",
                response.getQuestion().getQuestionText(),
                response.getResponseText(),
                response.getSpecificityScore() != null ? response.getSpecificityScore() : 0.0,
                response.getDepthScore() != null ? response.getDepthScore() : 0.0
            ))
            .collect(Collectors.joining("\n\n"));

        return String.format(
            "You are an expert career counselor providing personalized skill development advice. " +
            "Use the following retrieved knowledge from our verified skill database to ensure your suggestions are accurate and grounded in real learning resources.\n\n" +
            "RETRIEVED KNOWLEDGE:\n%s\n\n" +
            "USER ASSESSMENT RESPONSES:\n%s\n\n" +
            "Based on the retrieved knowledge and user responses, provide 3-5 specific, actionable suggestions for skill improvement. " +
            "Ensure suggestions reference actual skills, dependencies, and resources from the knowledge base. " +
            "Avoid suggesting skills or resources not mentioned in the retrieved knowledge. " +
            "Focus on practical next steps that build upon the user's current abilities.",
            retrievedKnowledge, responsesSummary
        );
    }

    /**
     * Creates an augmented prompt combining user responses, retrieved knowledge, and cognitive biases
     */
    private String createAugmentedPromptWithBiases(List<Response> responses, String retrievedKnowledge,
                                                   List<CognitiveBiasAnalysisService.CognitiveBiasResult> biases) {
        String responsesSummary = responses.stream()
            .map(response -> String.format(
                "Question: %s\nResponse: %s\nSpecificity Score: %.2f\nDepth Score: %.2f\nConfidence: %.2f\nConsistency: %.2f",
                response.getQuestion().getQuestionText(),
                response.getResponseText(),
                response.getSpecificityScore() != null ? response.getSpecificityScore() : 0.0,
                response.getDepthScore() != null ? response.getDepthScore() : 0.0,
                response.getConfidenceLevel() != null ? response.getConfidenceLevel() : 0.0,
                response.getConsistencyScore() != null ? response.getConsistencyScore() : 0.0
            ))
            .collect(Collectors.joining("\n\n"));

        String biasesSummary = biases.stream()
            .map(bias -> String.format(
                "Bias Type: %s\nLevel: %s\nScore: %.2f\nEvidence: %s\nImpact: %s\nRecommendations: %s",
                bias.getType(), bias.getLevel(), bias.getScore(),
                bias.getEvidence(), bias.getImpact(),
                String.join(", ", bias.getRecommendations())
            ))
            .collect(Collectors.joining("\n\n"));

        return String.format(
            "You are an expert career counselor using Neuro-RAG analysis to provide personalized skill development advice. " +
            "Use the following retrieved knowledge from our verified skill database and cognitive bias analysis to ensure your suggestions are accurate, grounded in real learning resources, and address the user's cognitive patterns.\n\n" +
            "RETRIEVED KNOWLEDGE:\n%s\n\n" +
            "USER ASSESSMENT RESPONSES:\n%s\n\n" +
            "COGNITIVE BIAS ANALYSIS:\n%s\n\n" +
            "Based on the retrieved knowledge, user responses, and cognitive bias analysis, provide 3-5 specific, actionable suggestions for skill improvement. " +
            "Ensure suggestions reference actual skills, dependencies, and resources from the knowledge base. " +
            "Address the identified cognitive biases by suggesting strategies to overcome them. " +
            "Focus on practical next steps that build upon the user's current abilities while accounting for their cognitive patterns. " +
            "Make suggestions that are tailored to help the user develop more effective thinking and learning strategies.",
            retrievedKnowledge, responsesSummary, biasesSummary
        );
    }
}
