package com.exam.system.service;

import com.exam.system.ws.TeacherRiskWebSocketHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class TeacherRiskPushService {

    private final TeacherRiskWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    public TeacherRiskPushService(TeacherRiskWebSocketHandler webSocketHandler, ObjectMapper objectMapper) {
        this.webSocketHandler = webSocketHandler;
        this.objectMapper = objectMapper;
    }

    public void pushCheatEvent(Long examId, Long studentId, String type, Integer durationSeconds) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("kind", "CHEAT_EVENT");
        payload.put("examId", examId);
        payload.put("studentId", studentId);
        payload.put("type", type);
        payload.put("durationSeconds", durationSeconds);
        payload.put("ts", System.currentTimeMillis());
        try {
            webSocketHandler.broadcast(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException ignore) {
            // Ignore serialization failure to avoid blocking exam flow.
        }
    }
}
