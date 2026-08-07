package com.gly091020.SableRagdollLib.network;

import com.gly091020.SableRagdollLib.SableRagdollLib;
import com.gly091020.SableRagdollLib.client.RagdollControlClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 服务端 → 客户端：木偶模式开关。客户端收到 true 后开始拦截自身移动并上报输入。
 */
public record ClientboundRagdollControlPacket(boolean controlling) implements CustomPacketPayload {

    public static final Type<ClientboundRagdollControlPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SableRagdollLib.MODID, "ragdoll_control_state"));

    public static final StreamCodec<FriendlyByteBuf, ClientboundRagdollControlPacket> STREAM_CODEC =
            StreamCodec.of(ClientboundRagdollControlPacket::encode, ClientboundRagdollControlPacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(FriendlyByteBuf buf, ClientboundRagdollControlPacket packet) {
        buf.writeBoolean(packet.controlling());
    }

    private static ClientboundRagdollControlPacket decode(FriendlyByteBuf buf) {
        return new ClientboundRagdollControlPacket(buf.readBoolean());
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) {
                RagdollControlClient.setControlling(controlling());
            }
        });
    }
}