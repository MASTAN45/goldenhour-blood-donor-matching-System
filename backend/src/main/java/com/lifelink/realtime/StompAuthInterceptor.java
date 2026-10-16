package com.lifelink.realtime;

import com.lifelink.security.AuthUser;
import com.lifelink.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/** Authenticates the STOMP CONNECT frame with the JWT and allows subscriptions only to private user queues. */
@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {
    private final JwtService jwt;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor acc = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (acc == null || acc.getCommand() == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(acc.getCommand())) {
            String header = acc.getFirstNativeHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                throw new MessagingException("Missing token");
            }
            try {
                AuthUser u = jwt.parse(header.substring(7));
                acc.setUser(new StompUser(String.valueOf(u.id())));
            } catch (Exception e) {
                throw new MessagingException("Invalid or expired token");
            }
        } else if (StompCommand.SUBSCRIBE.equals(acc.getCommand())) {
            String dest = acc.getDestination();
            if (dest == null || !dest.startsWith("/user/queue/")) {
                throw new MessagingException("Subscription not allowed");
            }
        }
        return message;
    }
}