package com.exam.system.config;

import com.exam.system.ws.TeacherRiskWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final TeacherRiskWebSocketHandler teacherRiskWebSocketHandler;
    private final TeacherRiskWsAuthInterceptor teacherRiskWsAuthInterceptor;

    public WebSocketConfig(TeacherRiskWebSocketHandler teacherRiskWebSocketHandler,
                           TeacherRiskWsAuthInterceptor teacherRiskWsAuthInterceptor) {
        this.teacherRiskWebSocketHandler = teacherRiskWebSocketHandler;
        this.teacherRiskWsAuthInterceptor = teacherRiskWsAuthInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(teacherRiskWebSocketHandler, "/ws/teacher-risk")
                .addInterceptors(teacherRiskWsAuthInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
