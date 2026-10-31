package com.gly091020.SableRagdollLib.api.control;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地集成服务器与客户端共享的布娃娃落脚点调试快照。
 */
public final class RagdollControlDebug {
    private static final Map<UUID, Snapshot> SNAPSHOTS = new ConcurrentHashMap<>();

    private RagdollControlDebug() {
    }

    public static void update(Snapshot snapshot) {
        SNAPSHOTS.put(snapshot.playerId(), snapshot);
    }

    public static void remove(UUID playerId) {
        SNAPSHOTS.remove(playerId);
    }

    public static List<Snapshot> snapshots() {
        return List.copyOf(SNAPSHOTS.values());
    }

    public record Snapshot(
            UUID playerId,
            ResourceKey<Level> dimension,
            long gameTime,
            @Nullable Vec3 stanceFoothold,
            @Nullable Vec3 nextFoothold,
            @Nullable Vec3 leftFoot,
            @Nullable Vec3 rightFoot,
            @Nullable Vec3 footRayOrigin,
            @Nullable Vec3 footRayEnd,
            boolean footRayHit,
            @Nullable Vec3 bodyAnchor,
            @Nullable Vec3 bodyTarget
    ) {
    }
}
