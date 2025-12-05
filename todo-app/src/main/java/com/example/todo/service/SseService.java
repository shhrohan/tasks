package com.example.todo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Log4j2
public class SseService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        // Timeout set to 0 (infinite) or a very large number to keep connection open
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitter.onCompletion(() -> {
            log.debug("SSE Emitter completed");
            emitters.remove(emitter);
        });

        emitter.onTimeout(() -> {
            log.debug("SSE Emitter timed out");
            emitter.complete();
            emitters.remove(emitter);
        });

        emitter.onError((e) -> {
            log.debug("SSE Emitter error: {}", e.getMessage());
            emitter.complete();
            emitters.remove(emitter);
        });

        emitters.add(emitter);
        log.info("SSE Connection Established. Client ID: {}", emitter.hashCode());
        log.info("New SSE client subscribed. Total active clients: {}", emitters.size());

        // Send initial event to force header flush and confirm connection to client
        try {
            emitter.send(SseEmitter.event().name("init").data("Connection established"));
        } catch (IOException e) {
            log.error("Failed to send init event to client {}", emitter.hashCode());
            emitter.completeWithError(e);
            emitters.remove(emitter);
        }

        return emitter;
    }

    public void broadcast(String eventName, Object data) {
        log.debug("Broadcasting SSE event: {}", eventName);
        List<SseEmitter> deadEmitters = new java.util.ArrayList<>();

        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                log.debug("Failed to send SSE event, removing emitter");
                deadEmitters.add(emitter);
            }
        });

        if (!deadEmitters.isEmpty()) {
            emitters.removeAll(deadEmitters);
            log.debug("Removed {} dead SSE emitters", deadEmitters.size());
        }
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        if (emitters.isEmpty())
            return;

        log.debug("Sending SSE heartbeat to {} clients", emitters.size());
        List<SseEmitter> deadEmitters = new java.util.ArrayList<>();

        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        });

        if (!deadEmitters.isEmpty()) {
            emitters.removeAll(deadEmitters);
            log.debug("Removed {} dead SSE emitters during heartbeat", deadEmitters.size());
        }
    }
}
