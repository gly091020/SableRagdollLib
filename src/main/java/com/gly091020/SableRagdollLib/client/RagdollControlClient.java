package com.gly091020.SableRagdollLib.client;

import com.gly091020.SableRagdollLib.network.ServerboundRagdollControlInputPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

/**
 * 客户端木偶模式：
 * <ul>
 *     <li>控制期间在 {@link MovementInputUpdateEvent} 中把 WASD 换算成世界方向并记录，
 *     然后清空玩家输入，冻结玩家自身移动；</li>
 *     <li>每 tick 把记录到的输入通过
 *     {@link ServerboundRagdollControlInputPacket} 发给服务端驱动布娃娃。</li>
 * </ul>
 */
public class RagdollControlClient {
    private static boolean controlling = false;
    private static float moveX;
    private static float moveZ;
    private static boolean moving;

    private RagdollControlClient() {
    }

    public static boolean isControlling() {
        return controlling;
    }

    public static void setControlling(boolean value) {
        controlling = value;
        if (!value) {
            moveX = 0;
            moveZ = 0;
            moving = false;
        }
    }

    /** 客户端每 tick（本地玩家更新移动输入时）调用。 */
    public static void handleMovementInput(MovementInputUpdateEvent event) {
        if (!controlling) {
            return;
        }
        Input input = event.getInput();
        var player = event.getEntity();
        // 世界方向：forward 按玩家朝向旋转，strafe 为左方向
        double yaw = Math.toRadians(player.getYRot());
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double strafeX = Math.cos(yaw);
        double strafeZ = Math.sin(yaw);
        moveX = (float) (input.forwardImpulse * forwardX + input.leftImpulse * strafeX);
        moveZ = (float) (input.forwardImpulse * forwardZ + input.leftImpulse * strafeZ);
        moving = input.forwardImpulse != 0.0F || input.leftImpulse != 0.0F;

        // 冻结玩家自身移动：清空移动输入与跳跃
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
    }

    /** 客户端每 tick 调用：控制期间把输入发包给服务端。 */
    public static void tick() {
        if (!controlling) {
            return;
        }
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            setControlling(false);
            return;
        }
        mc.getConnection().send(new ServerboundCustomPayloadPacket(
                new ServerboundRagdollControlInputPacket(moveX, moveZ, moving, mc.player.getYRot())));
    }
}