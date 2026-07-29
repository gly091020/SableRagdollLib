package com.gly091020.SableRagdollLib.client.button;

import com.gly091020.SableRagdollLib.SableRagdollLibConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

public class AllButtons {
    private static final Map<String, ButtonEntry> BUTTONS = new HashMap<>();
    public static void registry(String id, Component text, Button.OnPress onPress){
        BUTTONS.put(id, new ButtonEntry(text, onPress));
    }

    public static ButtonEntry get(String id){
        return BUTTONS.get(id);
    }

    static {
        AllButtons.registry("open_lib_config", Component.translatable("text.sableragdolllib.open_lib_config"), button ->
                Minecraft.getInstance().setScreen(AutoConfig.getConfigScreen(SableRagdollLibConfig.class, Minecraft.getInstance().screen).get()));
    }
}
