package com.gly091020.SableRagdollLib.editor;

import com.gly091020.SableRagdollLib.common.RagdollReloadListener;
import com.lowdragmc.lowdraglib2.editor.project.ProjectType;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.menu.FileMenu;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;

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
                        var fileName = r.getName();
                        ragdollProjectTypes.stream()
                                .filter(type -> fileName.endsWith(type.getSuffix()))
                                .findFirst()
                                .ifPresent(type -> {
                                    try {
                                        var project = type.loadProjectFromFile(r);
                                        editor.loadProject(project, r);
                                    } catch (Exception e) {
                                        Dialog.showNotification("editor.error", "editor.loading_failed", null).show(editor);
                                    }
                                });
                    }
                }).show(editor);
    }

    @Override
    public void addProjectProvider(ProjectType projectType) {
        super.addProjectProvider(projectType);
        ragdollProjectTypes.add(projectType);
    }
}
