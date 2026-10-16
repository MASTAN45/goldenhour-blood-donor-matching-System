package com.lifelink.realtime;

import com.lifelink.dto.RequestDtos.NearbyView;
import java.util.List;

public record AlertsCreatedEvent(List<Target> targets) {
    public record Target(Long donorUserId, NearbyView view) {
    }
}