package com.yoganavi.signaling.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SessionManager {

    private final Map<String, List<UUID>> rooms = new ConcurrentHashMap<>();
    private final Map<UUID, WebSocketSession> clients = new ConcurrentHashMap<>();
    private final Map<String, WebRTCSessionState> roomStates = new ConcurrentHashMap<>();
    private final Map<UUID, ClientState> clientStates = new ConcurrentHashMap<>();

    public enum WebRTCSessionState {
        Active, Creating, Ready, Impossible
    }

    public enum ClientState {
        CONNECTED, OFFER_SENT
    }

    public enum MessageType {
        STATE, OFFER, ANSWER, ICE
    }

    public void onSessionStarted(UUID sessionId, WebSocketSession session, String roomId)
        throws IOException {
        if ((rooms.getOrDefault(roomId, new ArrayList<>()).size()) >= 2) {
            session.close();
            return;
        }

        rooms.computeIfAbsent(roomId, k -> new ArrayList<>()).add(sessionId);
        clients.put(sessionId, session);
        clientStates.put(sessionId, ClientState.CONNECTED);

        log.info("세션 시작: 세션ID={}, 방ID={}", sessionId, roomId);
        log.info("현재 방 인원: {} 명", rooms.get(roomId).size());

        sendMessage(session, "클라이언트로 추가됨. sessionId: " + sessionId);
        updateRoomState(roomId);
    }

    private void updateRoomState(String roomId) throws IOException {
        int roomSize = rooms.getOrDefault(roomId, new ArrayList<>()).size();

        roomStates.put(roomId,
            roomSize == 2 ? WebRTCSessionState.Ready : WebRTCSessionState.Impossible);
        notifyAboutStateUpdate(roomId);
    }

    public void onMessage(UUID sessionId, String message, String roomId) throws IOException {
        if (message.startsWith(MessageType.STATE.toString())) {
            handleState(sessionId, roomId);
        } else if (message.startsWith(MessageType.OFFER.toString())) {
            handleOffer(sessionId, message, roomId);
        } else if (message.startsWith(MessageType.ANSWER.toString())) {
            handleAnswer(sessionId, message, roomId);
        } else if (message.startsWith(MessageType.ICE.toString())) {
            handleIce(sessionId, message, roomId);
        }
    }

    private void handleState(UUID sessionId, String roomId) throws IOException {
        WebSocketSession session = clients.get(sessionId);
        if (session != null) {
            sendMessage(session, MessageType.STATE + " " + roomStates.get(roomId));
        }
    }

    private void handleOffer(UUID sessionId, String message, String roomId) throws IOException {
        if (roomStates.get(roomId) != WebRTCSessionState.Ready) {
            return;
        }
        roomStates.put(roomId, WebRTCSessionState.Creating);
        log.info("Offer 요청 처리: 세션ID={}", sessionId);
        notifyAboutStateUpdate(roomId);

        UUID otherClientId = rooms.get(roomId).stream()
            .filter(id -> !id.equals(sessionId))
            .findFirst()
            .orElse(null);

        if (otherClientId != null) {
            WebSocketSession otherClient = clients.get(otherClientId);
            if (otherClient != null) {
                sendMessage(otherClient, message);
            }
        }

        clientStates.put(sessionId, ClientState.OFFER_SENT);
    }

    private void handleAnswer(UUID sessionId, String message, String roomId) throws IOException {
        if (roomStates.get(roomId) != WebRTCSessionState.Creating) {
            return;
        }
        log.info("Answer 응답 처리: 세션ID={}", sessionId);

        UUID otherClientId = rooms.get(roomId).stream()
            .filter(id -> !id.equals(sessionId))
            .findFirst()
            .orElse(null);

        if (otherClientId != null) {
            WebSocketSession otherClient = clients.get(otherClientId);
            if (otherClient != null) {
                sendMessage(otherClient, message);
            }
        }

        roomStates.put(roomId, WebRTCSessionState.Active);
        clientStates.put(sessionId, ClientState.CONNECTED);
        notifyAboutStateUpdate(roomId);
    }

    private void handleIce(UUID sessionId, String message, String roomId) throws IOException {
        log.info("ICE 후보 처리: 세션ID={}", sessionId);

        UUID otherClientId = rooms.get(roomId).stream()
            .filter(id -> !id.equals(sessionId))
            .findFirst()
            .orElse(null);

        if (otherClientId != null) {
            WebSocketSession otherClient = clients.get(otherClientId);
            if (otherClient != null) {
                sendMessage(otherClient, message);
            }
        }
    }

    public void onSessionClose(UUID sessionId, String roomId) throws IOException {
        clients.remove(sessionId);
        clientStates.remove(sessionId);
        List<UUID> roomClients = rooms.get(roomId);
        if (roomClients != null) {
            roomClients.remove(sessionId);
            if (roomClients.isEmpty()) {
                rooms.remove(roomId);
                roomStates.remove(roomId);
            } else {
                resetRoom(roomId);
            }
        }
    }

    private void resetRoom(String roomId) throws IOException {
        roomStates.put(roomId, WebRTCSessionState.Impossible);

        List<UUID> roomClients = rooms.get(roomId);
        if (roomClients != null) {
            for (UUID sessionId : roomClients) {
                clientStates.put(sessionId, ClientState.CONNECTED);
            }
        }
        updateRoomState(roomId);
    }

    private void notifyAboutStateUpdate(String roomId) throws IOException {
        WebRTCSessionState state = roomStates.getOrDefault(roomId, WebRTCSessionState.Impossible);
        List<UUID> roomClients = rooms.get(roomId);

        if (roomClients != null) {
            for (UUID sessionId : roomClients) {
                log.info("상태 업데이트: 세션ID={}, 상태={}", sessionId, state);
                WebSocketSession client = clients.get(sessionId);
                if (client != null) {
                    sendMessage(client, MessageType.STATE + " " + state);
                }
            }
        }
    }

    private void sendMessage(WebSocketSession session, String message) throws IOException {
        session.sendMessage(new TextMessage(message));
    }
}