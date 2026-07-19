package com.gly091020.SableRagdollLib.editor.util;

import com.gly091020.SableRagdollLib.command.SableRagdollLibClientCommand;
import com.gly091020.SableRagdollLib.common.ServerGetter;
import com.gly091020.SableRagdollLib.editor.RagdollEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.Locale;

public enum EditorCommands {
    RELOAD, PAUSE, DEBUG_RENDER;

    public void run(RagdollEditor editor){
        var server = ServerGetter.server;
        if(server == null)return;
        var player = server.getPlayerList().getPlayer(Minecraft.getInstance().getGameProfile().getId());
        if(player == null)return;

        switch (this){
            case RELOAD -> player.server.getCommands().performPrefixedCommand(
                    player.createCommandSourceStack(),
                    "sable_ragdoll_lib reload"
            );
            case PAUSE -> player.server.getCommands().performPrefixedCommand(
                    player.createCommandSourceStack(),
                    "sable paused"
            );
            case DEBUG_RENDER -> SableRagdollLibClientCommand.debugRender = !SableRagdollLibClientCommand.debugRender;
        }

        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1));
    }

    public Component getName(){
        return Component.translatable("text.sableragdolllib.command." + name().toLowerCase(Locale.ROOT));
    }
}
