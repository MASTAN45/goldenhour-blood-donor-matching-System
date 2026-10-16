package com.lifelink.realtime;

import java.util.List;

public record RequestClosedEvent(Long requestId, String type, List<Long> donorIds) {
}