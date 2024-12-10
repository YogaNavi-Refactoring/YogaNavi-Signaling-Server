package com.yoganavi.signaling.handler;

import com.yoganavi.signaling.service.SessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import java.util.UUID;

@Slf4j
@Component
public class SignalingWebSocketHandler implements WebSocketHandler {
    private final SessionManager sessionManager;

    public SignalingWebSocketHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        UUID sessionId = UUID.randomUUID();
        String liveId = session.getUri().getQuery().split("=")[1];  // liveId 파라미터 추출
        String isManager = session.getHandshakeHeaders().getFirst("isMyClass");

        log.info("입장 로그 {} {} {}", sessionId, liveId, isManager);

        /* TODO: 카프카 구현
        if ("1".equals(isManager)) {
            // Kafka 메시지 발행
            // LiveStatusMessage message = new LiveStatusMessage(liveId, true);
            // kafkaTemplate.send("live-status-topic", message);
        }
        */

        sessionManager.onSessionStarted(sessionId, session, liveId);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (message instanceof TextMessage) {
            String sessionId = session.getId();
            String liveId = session.getUri().getQuery().split("=")[1];
            String payload = ((TextMessage) message).getPayload();
            sessionManager.onMessage(UUID.fromString(sessionId), payload, liveId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        String liveId = session.getUri().getQuery().split("=")[1];
        String isManager = session.getHandshakeHeaders().getFirst("isMyClass");

        /* TODO: 카프카 구현
        if ("1".equals(isManager)) {
            // 카프카 메시지 발행
            // LiveStatusMessage message = new LiveStatusMessage(liveId, false);
            // kafkaTemplate.send("live-status-topic", message);
        }
        */

        sessionManager.onSessionClose(UUID.fromString(sessionId), liveId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error: {}", exception.getMessage(), exception);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}