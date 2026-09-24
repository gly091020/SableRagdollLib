package com.gly091020.SableRagdollLib.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class SwitchControlModeEvent extends Event implements ICancellableEvent {
    private final ServerPlayer serverPlayer;

    public SwitchControlModeEvent(ServerPlayer serverPlayer){
        this.serverPlayer = serverPlayer;
    }

    public ServerPlayer getServerPlayer() {
        return serverPlayer;
    }
}
