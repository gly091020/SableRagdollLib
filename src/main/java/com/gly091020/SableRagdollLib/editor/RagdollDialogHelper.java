package com.gly091020.SableRagdollLib.editor;

import com.gly091020.SableRagdollLib.api.RagdollTypeRegistry;
import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public class RagdollDialogHelper {
    public static Dialog createNewRagdollDialog(IProject project, Consumer<ResourceLocation> result){
        var textField1 = new TextField().setText("", false);
        var select = new Selector<ResourceLocation>();
        select.setCandidates(RagdollTypeRegistry.getAllType().stream().toList());
        textField1.setResourceLocationOnly();
        var dialog = new Dialog();
        dialog.setTitle(Component.translatable("text.sableragdolllib.open_file.input_id").getString());
        dialog.addContent(textField1.layout(layout -> layout.widthPercent(100)));
        dialog.addContent(select.layout(layout -> layout.widthPercent(100)));

        var ok = new Button()
                .setOnClick(e -> {
                    if(ResourceLocation.tryParse(textField1.getRawText()) == null)return;
                    if(select.getValue() == null)return;
                    result.accept(ResourceLocation.parse(textField1.getText()));
                    if(project instanceof RagdollProject ragdollProject)
                        ragdollProject.file.setType(select.getValue());
                    dialog.close();
                })
                .setText("ldlib.gui.tips.confirm")
                .addClass("__confirm-button__");
        dialog.addButton(ok);
        dialog.addButton(new Button()
                .setOnClick(e -> dialog.close())
                .setText("ldlib.gui.tips.cancel")
                .addClass("__cancel-button__"));

        return dialog;
    }
}
