package com.gly091020.SableRagdollLib.network;

import com.gly091020.SableRagdollLib.SableRagdollLib;
import com.gly091020.SableRagdollLib.api.RagdollDragManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * 客户端 -> 服务端的布娃娃拖拽数据包。
 * <p>
 * 坐标约定：
 * <ul>
 *     <li>{@code anchor}：抓取点在子维度嵌入式维度的方块坐标（客户端射线命中点所在的空间，
 *     与 Sable 击打系统传给 {@code applyImpulse} 的位置是同一空间）。</li>
 *     <li>{@code target}：拖拽目标点，使用主世界坐标；服务端会用子维度姿态投影回局部坐标。</li>
 * </ul>
 */
public record ServerboundDragRagdollPacket(Action action, UUID subLevel, Vec3 anchor, Vec3 target) implements CustomPacketPayload {

    public static final Type<ServerboundDragRagdollPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SableRagdollLib.MODID, "drag_ragdoll"));

    public static final StreamCodec<FriendlyByteBuf, ServerboundDragRagdollPacket> STREAM_CODEC =
            StreamCodec.of(ServerboundDragRagdollPacket::encode, ServerboundDragRagdollPacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(FriendlyByteBuf buf, ServerboundDragRagdollPacket packet) {
        buf.writeByte(packet.action().ordinal());
        buf.writeUUID(packet.subLevel());
        buf.writeDouble(packet.anchor().x);
        buf.writeDouble(packet.anchor().y);
        buf.writeDouble(packet.anchor().z);
        buf.writeDouble(packet.target().x);
        buf.writeDouble(packet.target().y);
        buf.writeDouble(packet.target().z);
    }

    private static ServerboundDragRagdollPacket decode(FriendlyByteBuf buf) {
        int actionId = buf.readUnsignedByte();
        if (actionId >= Action.values().length)
            throw new IllegalArgumentException("非法拖拽动作: " + actionId);
        var action = Action.values()[actionId];
        var subLevel = buf.readUUID();
        var anchor = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        var target = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        return new ServerboundDragRagdollPacket(action, subLevel, anchor, target);
    }

    public void handle(IPayloadContext context) {
        Player player = context.player();
        if (player == null) return;
        context.enqueueWork(() -> {
            if (!(player.level() instanceof ServerLevel serverLevel)) return;
            switch (action) {
                case START -> RagdollDragManager.startDrag(serverLevel, player, subLevel, anchor, target);
                case UPDATE -> RagdollDragManager.updateDrag(serverLevel, player, target);
                case END -> RagdollDragManager.endDrag(serverLevel, player);
            }
        });
    }

    public enum Action {
        START, UPDATE, END
    }
}
