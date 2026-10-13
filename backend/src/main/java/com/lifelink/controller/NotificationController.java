package com.lifelink.controller;

import com.lifelink.entity.NotificationLog;
import com.lifelink.repository.NotificationLogRepository;
import com.lifelink.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationLogRepository notifications;

    public record NotificationView(Long id, String title, String message, Long requestId, boolean read,
                                   LocalDateTime createdAt) {
    }

    @GetMapping
    public List<NotificationView> list(@AuthenticationPrincipal AuthUser user) {
        return notifications.findByUserIdOrderByCreatedAtDesc(user.id()).stream().limit(100)
                .map(n -> new NotificationView(n.getId(), n.getTitle(), n.getMessage(), n.getRequestId(),
                        n.isReadFlag(), n.getCreatedAt())).toList();
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread(@AuthenticationPrincipal AuthUser user) {
        return Map.of("unread", notifications.countByUserIdAndReadFlagFalse(user.id()));
    }

    @PatchMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        NotificationLog n = notifications.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!n.getUserId().equals(user.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your notification");
        }
        n.setReadFlag(true);
        notifications.save(n);
    }
}