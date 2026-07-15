package com.gly091020.SableRagdollLib.editor.util;

import com.gly091020.SableRagdollLib.common.DefFileLoader;
import com.gly091020.SableRagdollLib.common.RagdollReloadListener;
import com.gly091020.SableRagdollLib.editor.project.RagdollProjectType;
import com.gly091020.SableRagdollLib.resource.file.RagdollDefFile;
import com.lowdragmc.lowdraglib2.editor.project.ProjectType;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.menu.FileMenu;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.mojang.serialization.JsonOps;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class RagdollFileMenu extends FileMenu {
    private final List<ProjectType> ragdollProjectTypes = new ArrayList<>();
    public RagdollFileMenu(Editor editor) {
        super(editor);
    }

    @Override
    protected void onOpenProject() {
        var suffixes = ragdollProjectTypes.stream().map(ProjectType::getSuffix).toArray(String[]::new);
        Dialog.showFileDialog("ldlib.gui.editor.tips.load_project", RagdollReloadListener.LOCAL_DIR.toFile(), true,
                Dialog.suffixFilter(suffixes), r -> {
                    if (r != null && r.isFile()) {
                        openFile(r);
                    }
                }).show(editor);
    }

    @Override
    protected TreeBuilder.Menu createDefaultMenu() {
        var menu = super.createDefaultMenu();
        menu.leaf(Icons.OPEN_FILE, "text.sableragdolllib.open_from_dp", this::onOpenDPProject);
        return menu;
    }

    protected void onOpenDPProject() {
        RagdollDialogHelper.createOpenRagdollDialog(resourceLocation -> {
            var path = RagdollReloadListener.LOCAL_DIR.resolve(resourceLocation.getNamespace()).resolve(resourceLocation.getPath() + ".json");
            if(path.toFile().isFile()){
                openFile(path.toFile());
                return;
            }
            try{
                var d = DefFileLoader.getDefFile(resourceLocation);
                if(d == null)throw new RuntimeException();
                Files.createDirectories(path.getParent());
                Files.writeString(path,
                        RagdollProjectType.GSON.toJson(RagdollDefFile.CODEC.encodeStart(JsonOps.INSTANCE, d).getOrThrow()),
                        StandardOpenOption.CREATE);
                openFile(path.toFile());
            }catch (Exception e){
                Dialog.showNotification("editor.error", "editor.loading_failed", null).show(editor);
            }
        }).show(editor);
    }

    private void openFile(File path) {
        var fileName = path.getName();
        ragdollProjectTypes.stream()
                .filter(type -> fileName.endsWith(type.getSuffix()))
                .findFirst()
                .ifPresent(type -> {
                    try {
                        var project = type.loadProjectFromFile(path);
                        editor.loadProject(project, path);
                    } catch (Exception e) {
                        Dialog.showNotification("editor.error", "editor.loading_failed", null).show(editor);
                    }
                });
    }

    @Override
    public void addProjectProvider(ProjectType projectType) {
        super.addProjectProvider(projectType);
        ragdollProjectTypes.add(projectType);
    }
}
