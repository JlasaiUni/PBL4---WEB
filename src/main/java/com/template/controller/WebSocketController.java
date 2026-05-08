package com.template.websocket;

import com.template.service.impl.PostServiceImpl;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;

/**
 * Controlador WebSocket (STOMP).
 *
 * Clientes se suscriben a:
 *   /topic/notifications  → broadcast general
 *   /user/queue/messages  → mensajes privados
 *
 * Envían mensajes a:
 *   /app/chat             → este controller
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    // ── Chat broadcast ────────────────────────────────────────
    @MessageMapping("/chat")
    @SendTo("/topic/chat")
    public ChatMessage handleChatMessage(@Payload ChatMessage message,
                                         Principal principal) {
        message.setSender(principal != null ? principal.getName() : "Anónimo");
        message.setTimestamp(LocalDateTime.now());
        log.debug("WS chat: {} → {}", message.getSender(), message.getContent());
        return message;
    }

    // ── Mensaje privado ───────────────────────────────────────
    @MessageMapping("/private")
    @SendToUser("/queue/messages")
    public ChatMessage handlePrivateMessage(@Payload ChatMessage message,
                                             Principal principal) {
        message.setSender(principal != null ? principal.getName() : "Sistema");
        message.setTimestamp(LocalDateTime.now());
        return message;
    }

    // ── Escucha eventos de aplicación → broadcast automático ──
    @EventListener
    public void onPostPublished(PostServiceImpl.PostPublishedEvent event) {
        NotificationMessage notification = NotificationMessage.builder()
                .type("NEW_POST")
                .message("Nuevo post publicado: " + event.getPost().getTitle())
                .payload(event.getPost().getId().toString())
                .timestamp(LocalDateTime.now())
                .build();

        // Envía a todos los suscriptores de /topic/notifications
        messagingTemplate.convertAndSend("/topic/notifications", notification);
        log.info("Notificación WS enviada por nuevo post: {}", event.getPost().getTitle());
    }

    // ── DTOs de mensajes ──────────────────────────────────────
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ChatMessage {
        private String content;
        private String sender;
        private String type;          // CHAT | JOIN | LEAVE
        private LocalDateTime timestamp;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class NotificationMessage {
        private String type;
        private String message;
        private String payload;
        private LocalDateTime timestamp;
    }
}