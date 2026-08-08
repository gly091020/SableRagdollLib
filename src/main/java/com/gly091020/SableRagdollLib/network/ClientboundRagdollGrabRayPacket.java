package com.gly091020.SableRagdollLib.network;

import com.gly091020.SableRagdollLib.SableRagdollLib;
import com.gly091020.SableRagdollLib.client.RagdollGrabRayRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 服务端 → 客户端：抓取射线（渲染用）。
 * 按住 Alt 瞄准期间每 tick 发送一次（visible=true）；离开瞄准状态发一次 visible=false 清除。
 * 客户端据此画出"手正方向"的探测射线与选中的抓取位置。
 */
public record ClientboundRagdollGrabRayPacket(Vec3 origin, Vec3 target, boolean hit, boolean visible) implements CustomPacketPayload {

    public static final Type<ClientboundRagdollGrabRayPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SableRagdollLib.MODID, "ragdoll_grab_ray"));

    public static final StreamCodec<FriendlyByteBuf, ClientboundRagdollGrabRayPacket> STREAM_CODEC =
            StreamCodec.of(ClientboundRagdollGrabRayPacket::encode, ClientboundRagdollGrabRayPacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(FriendlyByteBuf buf, ClientboundRagdollGrabRayPacket packet) {
        buf.writeDouble(packet.origin().x);
        buf.writeDouble(packet.origin().y);
        buf.writeDouble(packet.origin().z);
        buf.writeDouble(packet.target().x);
        buf.writeDouble(packet.target().y);
        buf.writeDouble(packet.target().z);
        buf.writeBoolean(packet.hit());
        buf.writeBoolean(packet.visible());
    }

    private static ClientboundRagdollGrabRayPacket decode(FriendlyByteBuf buf) {
        return new ClientboundRagdollGrabRayPacket(
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                buf.readBoolean(), buf.readBoolean());
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) {
                RagdollGrabRayRenderer.update(origin(), target(), hit(), visible());
            }
        });
    }
}