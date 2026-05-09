package com.template.controller;

import com.template.event.PostPublishedEvent;
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
 *   /topic/notifications  -> broadcast general
 *   /user/queue/messages  -> mensajes privados
 *   /topic/chat           -> sala de chat publica
 *
 * Envian mensajes a:
 *   /app/chat     -> chat publico
 *   /app/private  -> mensaje privado
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    // Chat broadcast (grupal, abierto a cualquier usuario)
    @MessageMapping("/chat")
    @SendTo("/topic/chat")
    public ChatMessage handleChatMessage(@Payload ChatMessage message,
                                         Principal principal) {
        // Prioridad: 1) usuario autenticado, 2) sender enviado por cliente (invitado),
        // 3) "Anonimo" como ultimo recurso.
        String resolvedSender;
        if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
            resolvedSender = principal.getName();
        } else if (message.getSender() != null && !message.getSender().isBlank()) {
            resolvedSender = sanitizeName(message.getSender());
        } else {
            resolvedSender = "Anonimo";
        }
        message.setSender(resolvedSender);
        message.setTimestamp(LocalDateTime.now());
        if (message.getType() == null || message.getType().isBlank()) {
            message.setType("CHAT");
        }
        log.debug("WS chat: {} -> {} ({})", message.getSender(), message.getContent(), message.getType());
        return message;
    }

    // Mensaje privado
    @MessageMapping("/private")
    @SendToUser("/queue/messages")
    public ChatMessage handlePrivateMessage(@Payload ChatMessage message,
                                             Principal principal) {
        message.setSender(principal != null ? principal.getName() : "Sistema");
        message.setTimestamp(LocalDateTime.now());
        return message;
    }

    /** Limpia el nombre proporcionado por un cliente anonimo. */
    private String sanitizeName(String raw) {
        String trimmed = raw.trim();
        if (trimmed.length() > 30) trimmed = trimmed.substring(0, 30);
        // Quitamos caracteres potencialmente peligrosos pero permitimos
        // letras (incluido acentos), digitos, espacios y guiones bajos/medios.
        return trimmed.replaceAll("[<>\"'`]", "");
    }

    // Escucha eventos de publicacion de posts y los broadcastea
    @EventListener
    public void onPostPublished(PostPublishedEvent event) {
        NotificationMessage notification = NotificationMessage.builder()
                .type("NEW_POST")
                .message("Nuevo post publicado: " + event.getPost().getTitle())
                .payload(event.getPost().getId().toString())
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/notifications", notification);
        log.info("Notificacion WS enviada por nuevo post: {}", event.getPost().getTitle());
    }

    // DTOs internos de mensajes
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
