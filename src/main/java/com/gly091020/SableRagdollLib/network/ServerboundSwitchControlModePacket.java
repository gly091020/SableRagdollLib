package com.gly091020.SableRagdollLib.network;

import com.gly091020.SableRagdollLib.SableRagdollLib;
import com.gly091020.SableRagdollLib.api.RagdollManager;
import com.gly091020.SableRagdollLib.api.control.RagdollControlManager;
import com.gly091020.SableRagdollLib.entity.PartSeat;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ServerboundSwitchControlModePacket() implements CustomPacketPayload {
    public static final Type<ServerboundSwitchControlModePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SableRagdollLib.MODID, "switch_control_mode"));
    public static final StreamCodec<ByteBuf, ServerboundSwitchControlModePacket> STREAM_CODEC = StreamCodec.of(
            (a, b) -> {}, a -> new ServerboundSwitchControlModePacket()
    );
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        if(RagdollControlManager.get(context.player()) != null)
            RagdollControlManager.stop(context.player());
        else {
            if(!(context.player().getVehicle() instanceof PartSeat partSeat))return;
            var rag = RagdollManager.get(context.player().level(), partSeat.getMainUUID());
            if(rag == null)return;
            RagdollControlManager.start((ServerPlayer) context.player(), rag);
        }
    }
}
