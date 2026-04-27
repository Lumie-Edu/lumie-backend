package com.lumie.auth.domain.vo;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Curated DiceBear notionists avatar seeds — kept in sync with
 * {@code lumie-frontend/src/shared/lib/avatar.ts AVATAR_SEEDS}.
 * On user creation a random seed is assigned so every new user has an avatar
 * out of the box; the user can later change it via the profile picker.
 */
public final class AvatarSeeds {

    public static final List<String> ALL = List.of(
            "Aurora", "Felix", "Mocha", "Sky", "Nova", "Sage", "Luna", "Atlas",
            "Zephyr", "Rio", "Jade", "Iris", "Cleo", "Milo", "Hazel", "Theo"
    );

    private AvatarSeeds() {}

    public static String randomSeed() {
        return ALL.get(ThreadLocalRandom.current().nextInt(ALL.size()));
    }
}
