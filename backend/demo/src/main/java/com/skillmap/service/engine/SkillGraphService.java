package com.skillmap.service.engine;

import com.skillmap.model.entity.Skill;
import com.skillmap.model.entity.SkillDependency;
import com.skillmap.repository.SkillDependencyRepository;
import com.skillmap.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkillGraphService {

    private final SkillRepository skillRepository;
    private final SkillDependencyRepository skillDependencyRepository;

    private Graph<String, DefaultEdge> skillGraph;
    private Map<String, Skill> skillMap;

    public void buildSkillGraph() {
        log.info("Building skill dependency graph");

        skillGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
        skillMap = new HashMap<>();

        // Add all skills as vertices
        skillRepository.findAll().forEach(skill -> {
            skillGraph.addVertex(skill.getSkillCode());
            skillMap.put(skill.getSkillCode(), skill);
        });

        // Add dependencies as edges (parent -> child)
        skillDependencyRepository.findAll().forEach(dependency -> {
            if (dependency.getParentSkill() != null && dependency.getChildSkill() != null) {
                skillGraph.addEdge(
                    dependency.getParentSkill().getSkillCode(),
                    dependency.getChildSkill().getSkillCode()
                );
            }
        });

        log.info("Skill graph built with {} vertices and {} edges",
                skillGraph.vertexSet().size(), skillGraph.edgeSet().size());
    }

    public List<String> getPrerequisiteSkills(String skillCode) {
        if (skillGraph == null) {
            buildSkillGraph();
        }

        List<String> prerequisites = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        getPrerequisitesRecursive(skillCode, prerequisites, visited);

        return prerequisites;
    }

    private void getPrerequisitesRecursive(String skillCode, List<String> prerequisites, Set<String> visited) {
        if (visited.contains(skillCode)) {
            return;
        }

        visited.add(skillCode);

        // Get all incoming edges (prerequisites)
        skillGraph.incomingEdgesOf(skillCode).forEach(edge -> {
            String prerequisite = skillGraph.getEdgeSource(edge);
            prerequisites.add(prerequisite);
            getPrerequisitesRecursive(prerequisite, prerequisites, visited);
        });
    }

    public List<String> getDependentSkills(String skillCode) {
        if (skillGraph == null) {
            buildSkillGraph();
        }

        List<String> dependents = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        getDependentsRecursive(skillCode, dependents, visited);

        return dependents;
    }

    private void getDependentsRecursive(String skillCode, List<String> dependents, Set<String> visited) {
        if (visited.contains(skillCode)) {
            return;
        }

        visited.add(skillCode);

        // Get all outgoing edges (dependents)
        skillGraph.outgoingEdgesOf(skillCode).forEach(edge -> {
            String dependent = skillGraph.getEdgeTarget(edge);
            dependents.add(dependent);
            getDependentsRecursive(dependent, dependents, visited);
        });
    }

    public List<String> getTopologicalOrder() {
        if (skillGraph == null) {
            buildSkillGraph();
        }

        List<String> orderedSkills = new ArrayList<>();
        TopologicalOrderIterator<String, DefaultEdge> iterator =
            new TopologicalOrderIterator<>(skillGraph);

        iterator.forEachRemaining(orderedSkills::add);

        return orderedSkills;
    }

    public boolean hasCycles() {
        if (skillGraph == null) {
            buildSkillGraph();
        }

        try {
            getTopologicalOrder();
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    public int getSkillLevel(String skillCode) {
        List<String> prerequisites = getPrerequisiteSkills(skillCode);
        return prerequisites.size() + 1; // Level based on number of prerequisites
    }

    public Map<String, Integer> getSkillLevels() {
        Map<String, Integer> levels = new HashMap<>();

        if (skillGraph == null) {
            buildSkillGraph();
        }

        skillGraph.vertexSet().forEach(skillCode -> {
            levels.put(skillCode, getSkillLevel(skillCode));
        });

        return levels;
    }
}
