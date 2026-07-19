package com.gly091020.SableRagdollLib.editor.util;

import com.gly091020.SableRagdollLib.api.RagdollTypeRegistry;
import com.gly091020.SableRagdollLib.common.DefFileLoader;
import com.gly091020.SableRagdollLib.editor.project.RagdollProject;
import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import com.mojang.datafixers.util.Pair;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class RagdollDialogHelper {
    public static Dialog createNewRagdollDialog(IProject project, Consumer<Pair<ResourceLocation, ResourceLocation>> result){
        var textField1 = new TextField().setText("", false);

        var label = new Label().setText(Component.translatable("text.sableragdolllib.open_file.base"));
        var searchComponent = getRegdollSearchComponent();
        var allBase = new UIElement();
        label.getLayout().widthPercent(10);
        label.getTextStyle().textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER);
        searchComponent.getLayout().widthPercent(90).marginBottom(100);
        allBase.addChildren(label, searchComponent);
        allBase.getLayout().widthPercent(100).flexDirection(FlexDirection.ROW);

        var select = new Selector<ResourceLocation>();
        select.setCandidates(RagdollTypeRegistry.getAllType().stream().toList());
        textField1.setResourceLocationOnly();
        var dialog = new Dialog();
        dialog.setTitle(Component.translatable("text.sableragdolllib.open_file.input_id").getString());
        dialog.addContent(textField1.layout(layout -> layout.widthPercent(100)));
        dialog.addContent(select.layout(layout -> layout.widthPercent(100)));
        dialog.addContent(allBase);
        dialog.overlay.getLayout().width(300).height(300);

        var ok = new Button()
                .setOnClick(e -> {
                    if(ResourceLocation.tryParse(textField1.getRawText()) == null)return;
                    if(select.getValue() == null)return;
                    result.accept(Pair.of(ResourceLocation.parse(textField1.getText()), searchComponent.getValue()));
                    if(project instanceof RagdollProject ragdollProject)
                        ragdollProject.file.type = select.getValue();
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

    private static @NotNull SearchComponent<ResourceLocation> getRegdollSearchComponent() {
        var searchComponent = new SearchComponent<ResourceLocation>();
        searchComponent.setSearchUI(new SearchComponent.ISearchUI<>() {
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
        return searchComponent;
    }

    public static Dialog createOpenRagdollDialog(Consumer<ResourceLocation> result){
        var dialog = new Dialog();
        var search = getRegdollSearchComponent();
        search.getLayout().marginBottom(100);
        dialog.setTitle(Component.translatable("text.sableragdolllib.open_file").getString());
        dialog.overlay.layout(layout -> {
            layout.width(300);
            layout.height(300);
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
