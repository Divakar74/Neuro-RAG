package com.skillmap.controller;

import com.skillmap.service.flowchart.FlowchartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/flowchart")
@RequiredArgsConstructor
public class FlowchartController {

    private final FlowchartService flowchartService;

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> getFlowchart(@PathVariable Long sessionId) {
        Map<String, Object> graph = flowchartService.buildFlowchart(sessionId);
        return ResponseEntity.ok(graph);
    }
}
