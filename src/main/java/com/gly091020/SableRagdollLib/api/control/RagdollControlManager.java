package com.gly091020.SableRagdollLib.api.control;

import com.gly091020.SableRagdollLib.api.Ragdoll;
import com.gly091020.SableRagdollLib.network.ClientboundRagdollControlPacket;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 布娃娃操控会话管理器。
 * <p>
 * 以玩家 UUID 为键，一个玩家同时只能控制一个布娃娃；新会话会先结束旧会话。
 * 会话开始/结束时向客户端发送 {@link ClientboundRagdollControlPacket}，
 * 让客户端开启"拦截移动 + 上报输入"的木偶模式。
 * 由主模组在服务端 tick 事件中调用 {@link #tick()}，服务器停止时调用 {@link #reset()}。
 */
public final class RagdollControlManager {
    private static final Map<UUID, RagdollControlSession> SESSIONS = new HashMap<>();

    private RagdollControlManager() {
    }

    /**
     * 开始控制：先结束该玩家已有的会话，再尝试创建新会话。
     *
     * @return 创建成功的会话，失败（如未识别出身体）返回 {@code null}
     */
    public static RagdollControlSession start(ServerPlayer player, Ragdoll ragdoll) {
        stop(player);
        if (player == null || ragdoll == null) {
            return null;
        }
        RagdollControlSession session = RagdollControlSession.create(player, ragdoll);
        if (session == null) {
            return null;
        }
        SESSIONS.put(player.getUUID(), session);
        sendState(player, true);
        return session;
    }

    public static RagdollControlSession start(ServerPlayer player, RagdollControlSession session){
        stop(player);
        if (player == null || session == null) {
            return null;
        }
        SESSIONS.put(player.getUUID(), session);
        sendState(player, true);
        return session;
    }

    public static RagdollControlSession get(Player player) {
        return player == null ? null : SESSIONS.get(player.getUUID());
    }

    /**
     * 结束指定玩家的操控会话（无会话时无操作）。
     */
    public static void stop(Player player) {
        if (player == null) {
            return;
        }
        RagdollControlSession session = SESSIONS.remove(player.getUUID());
        if (session != null) {
            session.dispose();
            sendState(session.getPlayer(), false);
        }
    }

    /**
     * 服务端每 tick 调用：驱动所有会话，并清理失效会话（约束已断、玩家死亡/离开等）。
     */
    public static void tick() {
        Iterator<Map.Entry<UUID, RagdollControlSession>> iterator = SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            RagdollControlSession session = iterator.next().getValue();
            session.tick();
            if (!session.isValid()) {
                iterator.remove();
                sendState(session.getPlayer(), false);
            }
        }
    }

    /**
     * 服务器停止/世界卸载时清理全部会话。
     */
    public static void reset() {
        for (RagdollControlSession session : SESSIONS.values()) {
            session.dispose();
            sendState(session.getPlayer(), false);
        }
        SESSIONS.clear();
    }

    private static void sendState(ServerPlayer player, boolean controlling) {
        if (player == null || player.connection == null) {
            return;
        }
        try {
            player.connection.send(new ClientboundCustomPayloadPacket(
                    new ClientboundRagdollControlPacket(controlling)));
        } catch (Exception ignored) {
        }
    }
}