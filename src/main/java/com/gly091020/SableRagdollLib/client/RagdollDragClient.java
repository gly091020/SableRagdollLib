package com.gly091020.SableRagdollLib.client;

import com.gly091020.SableRagdollLib.block.AbstractPartBlock;
import com.gly091020.SableRagdollLib.entity.PartSeat;
import com.gly091020.SableRagdollLib.network.ServerboundDragRagdollPacket;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 客户端拖拽逻辑：长按使用键（默认右键）拖拽正在注视的布娃娃部件。
 * <p>
 * 按下使用键时，若准星指向布娃娃部件方块（Sable 的射线会命中子维度方块），
 * 就发送 START 数据包并记录锚点；按住期间每 tick 把视线前方 {@value #DRAG_DISTANCE}
 * 方块处的点作为目标发送 UPDATE；松开时发送 END。
 */
public class RagdollDragClient {
    /** 拖拽时目标点与玩家眼睛的距离（方块） */
    private static final double DRAG_DISTANCE = 0.5;

    private static @Nullable UUID draggingSubLevel;
    private static Vec3 anchor = Vec3.ZERO;
    private static boolean keyDown = false;

    public static void tick() {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.level == null || mc.screen != null || player.isSpectator()) {
            keyDown = false;
            end();
            return;
        }

        boolean down = mc.options.keyUse.isDown();
        if (down && !keyDown) {
            keyDown = true;
            tryStart(mc, player);
        } else if (!down && keyDown) {
            keyDown = false;
            end();
        }

        if (down && draggingSubLevel != null) {
            send(ServerboundDragRagdollPacket.Action.UPDATE, draggingSubLevel, anchor, target(player));
        }
    }

    private static void tryStart(Minecraft mc, LocalPlayer player) {
        if(player.getVehicle() instanceof PartSeat)return;

        var hit = mc.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK || mc.level == null) return;
        var blockHit = (BlockHitResult) hit;

        // 确认命中的方块是布娃娃部件（部件方块位于子维度嵌入式维度，主世界按同样坐标取方块）
        if (!(mc.level.getBlockState(blockHit.getBlockPos()).getBlock() instanceof AbstractPartBlock)) return;
        var subLevel = SableCompanion.INSTANCE.getContaining(mc.level, hit.getLocation());
        if (subLevel == null) return;

        // 命中位置直接作为锚点（与 Sable 击打系统同一坐标空间）
        draggingSubLevel = subLevel.getUniqueId();
        anchor = hit.getLocation();
        send(ServerboundDragRagdollPacket.Action.START, draggingSubLevel, anchor, target(player));
    }

    private static Vec3 target(LocalPlayer player) {
        return player.getEyePosition().add(player.getLookAngle().scale(DRAG_DISTANCE));
    }

    private static void send(ServerboundDragRagdollPacket.Action action, UUID subLevel, Vec3 anchor, Vec3 target) {
        var mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;
        mc.getConnection().send(new ServerboundCustomPayloadPacket(
                new ServerboundDragRagdollPacket(action, subLevel, anchor, target)
        ));
    }

    private static void end() {
        if (draggingSubLevel != null) {
            send(ServerboundDragRagdollPacket.Action.END, draggingSubLevel, anchor, Vec3.ZERO);
        }
        draggingSubLevel = null;
        anchor = Vec3.ZERO;
    }

    public static boolean isDragging() {
        return draggingSubLevel != null;
    }
}
