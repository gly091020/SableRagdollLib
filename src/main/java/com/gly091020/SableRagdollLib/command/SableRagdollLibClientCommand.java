package com.gly091020.SableRagdollLib.command;

import com.gly091020.SableRagdollLib.SableRagdollLib;
import com.gly091020.SableRagdollLib.editor.EditorOpener;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;

public class SableRagdollLibClientCommand {
    private static final String COMMAND = "sable_ragdoll_lib_client";
    public static boolean debugRender = false;

    public static void registry(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal(COMMAND);

        root.then(Commands.literal("debugRender").executes(SableRagdollLibClientCommand::debugRender));
        if(SableRagdollLib.hasLDLib())
            root.then(Commands.literal("editor").executes(SableRagdollLibClientCommand::openEditor));

        dispatcher.register(root);
    }

    public static int debugRender(CommandContext<CommandSourceStack> context){
        debugRender = !debugRender;
        context.getSource().sendSuccess(() -> Component.translatable("command.sableragdolllib.debug_render.success"), false);
        return 1;
    }

    public static int openEditor(CommandContext<CommandSourceStack> context){
        EditorOpener.open();
        return 1;
    }
}
