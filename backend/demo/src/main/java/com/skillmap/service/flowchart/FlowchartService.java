package com.skillmap.service.flowchart;

import com.skillmap.model.entity.AssessmentSession;
import com.skillmap.model.entity.Skill;
import com.skillmap.model.entity.SkillAssessment;
import com.skillmap.model.entity.SkillDependency;
import com.skillmap.repository.AssessmentSessionRepository;
import com.skillmap.repository.SkillAssessmentRepository;
import com.skillmap.repository.SkillDependencyRepository;
import com.skillmap.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlowchartService {

    private final SkillRepository skillRepository;
    private final SkillDependencyRepository skillDependencyRepository;
    private final SkillAssessmentRepository skillAssessmentRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;

    public Map<String, Object> buildFlowchart(Long sessionId) {
        AssessmentSession session = assessmentSessionRepository.findById(sessionId).orElse(null);
        if (session == null) return Map.of("nodes", List.of(), "edges", List.of());

        // Load assessed levels
        List<SkillAssessment> assessments = skillAssessmentRepository.findBySession(session);
        Map<Long, Double> skillLevelById = new HashMap<>();
        for (SkillAssessment a : assessments) {
            skillLevelById.put(a.getSkill().getId(), a.getAssessedLevel());
        }

        // Nodes: each skill mapped to status
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (Skill s : skillRepository.findAll()) {
            double level = skillLevelById.getOrDefault(s.getId(), 0.5);
            String status = level < 0.4 ? "weak" : (level < 0.7 ? "moderate" : "strong");
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", s.getSkillCode());
            node.put("label", s.getDisplayName());
            node.put("status", status);
            node.put("level", level);
            nodes.add(node);
        }

        // Edges: dependencies determine learning order
        List<Map<String, String>> edges = new ArrayList<>();
        for (SkillDependency d : skillDependencyRepository.findAll()) {
            Map<String, String> e = new HashMap<>();
            // Edge from prerequisite (child) to parent (skill requiring it)
            e.put("source", d.getChildSkill().getSkillCode());
            e.put("target", d.getParentSkill().getSkillCode());
            edges.add(e);
        }

        // Optional compact list structure (for simple rendering)
        List<Map<String, Object>> list = nodes.stream().map(n -> {
            String code = (String) n.get("id");
            List<String> next = edges.stream()
                    .filter(e -> e.get("source").equals(code))
                    .map(e -> e.get("target")).collect(Collectors.toList());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("skill", n.get("label"));
            item.put("skillCode", code);
            item.put("status", n.get("status"));
            item.put("action", recommendedAction((String) n.get("status")));
            item.put("next", next);
            return item;
        }).collect(Collectors.toList());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("nodes", nodes);
        out.put("edges", edges);
        out.put("list", list);
        return out;
    }

    private String recommendedAction(String status) {
        return switch (status) {
            case "weak" -> "Start with foundation resources and guided tutorials.";
            case "moderate" -> "Practice targeted problems and small projects.";
            default -> "Deep-dive advanced topics and system-level projects.";
        };
    }
}
