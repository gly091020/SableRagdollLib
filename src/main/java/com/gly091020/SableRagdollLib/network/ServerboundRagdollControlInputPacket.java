package com.gly091020.SableRagdollLib.network;

import com.gly091020.SableRagdollLib.SableRagdollLib;
import com.gly091020.SableRagdollLib.api.control.RagdollControlManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端 → 服务端：木偶模式的移动输入。
 * <p>
 * 客户端在控制期间每 tick 发送一次；moveX/moveZ 为世界坐标下的水平方向
 * （由 WASD 与玩家朝向换算，未归一化），moving 表示是否在按移动键，
 * yaw 为玩家摄像机朝向（世界角度，度），用于让布娃娃面向玩家。
 */
public record ServerboundRagdollControlInputPacket(float moveX, float moveZ, boolean moving, float yaw) implements CustomPacketPayload {

    public static final Type<ServerboundRagdollControlInputPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SableRagdollLib.MODID, "ragdoll_control_input"));

    public static final StreamCodec<FriendlyByteBuf, ServerboundRagdollControlInputPacket> STREAM_CODEC =
            StreamCodec.of(ServerboundRagdollControlInputPacket::encode, ServerboundRagdollControlInputPacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(FriendlyByteBuf buf, ServerboundRagdollControlInputPacket packet) {
        buf.writeFloat(packet.moveX());
        buf.writeFloat(packet.moveZ());
        buf.writeBoolean(packet.moving());
        buf.writeFloat(packet.yaw());
    }

    private static ServerboundRagdollControlInputPacket decode(FriendlyByteBuf buf) {
        return new ServerboundRagdollControlInputPacket(buf.readFloat(), buf.readFloat(), buf.readBoolean(), buf.readFloat());
    }

    public void handle(IPayloadContext context) {
        Player player = context.player();
        if (player == null) {
            return;
        }
        context.enqueueWork(() -> {
            var session = RagdollControlManager.get(player);
            if (session != null) {
                session.updateInput(moveX(), moveZ(), moving(), yaw());
            }
        });
    }
}