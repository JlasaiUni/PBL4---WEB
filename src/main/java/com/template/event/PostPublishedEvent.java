package com.template.event;

import com.template.entity.Post;
import org.springframework.context.ApplicationEvent;

/**
 * Evento publicado cuando un post cambia a estado publicado.
 * Los listeners pueden suscribirse con @EventListener para,
 * por ejemplo, enviar notificaciones WebSocket.
 */
public class PostPublishedEvent extends ApplicationEvent {

    private final Post post;

    public PostPublishedEvent(Object source, Post post) {
        super(source);
        this.post = post;
    }

    public Post getPost() {
        return post;
    }
}
