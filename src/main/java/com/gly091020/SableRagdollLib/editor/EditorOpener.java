package com.gly091020.SableRagdollLib.editor;

import com.lowdragmc.lowdraglib2.editor.ui.EditorWindow;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import net.minecraft.client.Minecraft;

public class EditorOpener {
    public static void open(){
        var window = EditorWindow.open(RagdollEditor.WINDOW_ID, RagdollEditor::new);
        var ui = new ModularUI(UI.of(window))
                .shouldCloseOnEsc(false)
                .shouldCloseOnKeyInventory(false);
        Minecraft.getInstance().setScreen(new ModularUIScreen(ui, window.getEditorName()));
    }
}
