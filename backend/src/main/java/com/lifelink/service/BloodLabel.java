package com.lifelink.service;

import com.lifelink.entity.BloodGroup;

/** O_NEGATIVE -> "O-", AB_POSITIVE -> "AB+" for user-facing text. */
public final class BloodLabel {
    private BloodLabel() {
    }

    public static String of(BloodGroup g) {
        return g.name().replace("_POSITIVE", "+").replace("_NEGATIVE", "-");
    }
}