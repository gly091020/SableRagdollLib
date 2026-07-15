package com.gly091020.SableRagdollLib.editor.util;

import com.gly091020.SableRagdollLib.api.RagdollTypeRegistry;
import com.gly091020.SableRagdollLib.common.DefFileLoader;
import com.gly091020.SableRagdollLib.editor.project.RagdollProject;
import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    public static Dialog createOpenRagdollDialog(Consumer<ResourceLocation> result){
        var dialog = new Dialog();
        var search = new SearchComponent<ResourceLocation>();
        search.setSearchUI(new SearchComponent.ISearchUI<>() {
            @Override
            public @NotNull String resultText(@NotNull ResourceLocation value) {return value.toString();}

            @Override
            public void onResultSelected(@Nullable ResourceLocation value) {}

            @Override
            public void search(String word, IResultHandler<ResourceLocation> searchHandler) {
                DefFileLoader.getAllKeys().stream()
                        .filter(rl -> word.isEmpty() || rl.toString().contains(word))
                        .forEach(searchHandler);
            }
        });
        dialog.setTitle(Component.translatable("text.sableragdolllib.open_file").getString());
        dialog.overlay.layout(layout -> {
            layout.width(300);
            layout.height(50);
        });
        dialog.addContent(search.layout(layout -> layout.widthPercent(100)));

        dialog.addButton(new Button()
                .setOnClick(e -> {
                    if(DefFileLoader.getDefFile(search.getValue()) == null)return;
                    result.accept(search.getValue());
                    dialog.close();
                })
                .setText("ldlib.gui.tips.confirm")
                .addClass("__confirm-button__"));
        dialog.addButton(new Button()
                .setOnClick(e -> dialog.close())
                .setText("ldlib.gui.tips.cancel")
                .addClass("__cancel-button__"));

        return dialog;
    }
}
