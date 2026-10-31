package com.gly091020.SableRagdollLib.network;

import com.gly091020.SableRagdollLib.SableRagdollLib;
import com.gly091020.SableRagdollLib.api.RagdollManager;
import com.gly091020.SableRagdollLib.api.control.RagdollControlManager;
import com.gly091020.SableRagdollLib.api.event.SwitchControlModeEvent;
import com.gly091020.SableRagdollLib.compat.player_ragdoll.PlayerRagdollUtil;
import com.gly091020.SableRagdollLib.compat.util.CompatMods;
import com.gly091020.SableRagdollLib.entity.PartSeat;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ServerboundSwitchControlModePacket(boolean held) implements CustomPacketPayload {
    public static final Type<ServerboundSwitchControlModePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SableRagdollLib.MODID, "switch_control_mode"));
    public static final StreamCodec<ByteBuf, ServerboundSwitchControlModePacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeBoolean(packet.held()),
            buf -> new ServerboundSwitchControlModePacket(buf.readBoolean())
    );
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        var player = (ServerPlayer) context.player();
        var session = RagdollControlManager.get(player);
        if (!held) {
            if (session != null && session.isRecovering()) {
                RagdollControlManager.stop(player);
            }
            return;
        }
        if(NeoForge.EVENT_BUS.post(new SwitchControlModeEvent(player)).isCanceled())return;
        if(session != null) {
            if (session.isRecovering()) {
                return;
            }
            RagdollControlManager.stop(player);
            return;
        } else {
            if(CompatMods.PLAYER_RAGDOLL && PlayerRagdollUtil.isRagdoll(player)){
                var started = PlayerRagdollUtil.startControl(player);
                if(started != null)return;
            }

            if(!(context.player().getVehicle() instanceof PartSeat partSeat))return;
            var rag = RagdollManager.get(context.player().level(), partSeat.getMainUUID());
            if(rag == null)return;
            RagdollControlManager.start((ServerPlayer) context.player(), rag);
        }
    }
}
