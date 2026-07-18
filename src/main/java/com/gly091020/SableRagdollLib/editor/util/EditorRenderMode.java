package com.gly091020.SableRagdollLib.editor.util;

import net.minecraft.network.chat.Component;

import java.util.Locale;

public enum EditorRenderMode {
    HITBOX, RENDERER, JOINT, EXPRESSION;

    public Component getName(){
        return Component.translatable("text.sableragdolllib.mode." + name().toLowerCase(Locale.ROOT));
    }
}
