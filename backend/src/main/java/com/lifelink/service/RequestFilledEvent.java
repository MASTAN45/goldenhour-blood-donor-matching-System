package com.lifelink.service;

import java.util.List;

public record RequestFilledEvent(Long requestId, Long requesterId, List<Long> closedDonorIds) {
}