package com.lifelink.realtime;

import java.security.Principal;

/** The WebSocket principal; its name is the user id, which routes /user/queue messages. */
public record StompUser(String name) implements Principal {
    @Override
    public String getName() {
        return name;
    }
}