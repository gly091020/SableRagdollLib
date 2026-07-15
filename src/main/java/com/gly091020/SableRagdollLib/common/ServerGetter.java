package com.gly091020.SableRagdollLib.common;

import net.minecraft.server.MinecraftServer;

import java.util.Optional;

public class ServerGetter {
    public static MinecraftServer server;

    public static Optional<MinecraftServer> getServer(){
        return Optional.ofNullable(server);
    }
}
