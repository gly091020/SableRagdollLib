package com.gly091020.SableRagdollLib.editor;

import com.gly091020.SableRagdollLib.SableRagdollLib;
import com.gly091020.SableRagdollLib.api.RagdollTypeRegistry;
import com.gly091020.SableRagdollLib.common.RagdollReloadListener;
import com.google.common.util.concurrent.Runnables;
import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class RagdollEditor extends Editor {
    public static final ResourceLocation WINDOW_ID = ResourceLocation.fromNamespaceAndPath(SableRagdollLib.MODID, "editor");
    public RagdollFileMenu ragdollFileMenu;

    @Override
    protected @NotNull Editor createNewEditorInstance() {
        return new RagdollEditor();
    }

    @Override
    protected void initMenus() {
        this.ragdollFileMenu = new RagdollFileMenu(this);
        fileMenu.addProjectProvider(RagdollProjectType.TYPE);
        ragdollFileMenu.addProjectProvider(RagdollProjectType.TYPE);
        menuContainer.addChildren(ragdollFileMenu.createMenuTab(), viewMenu.createMenuTab());
    }

    @Override
    protected void onPrepareResourceView() {

    }

    @Override
    protected void onPrepareHistoryView() {

    }

    @Override
    protected void loadNewProject(IProject project, @Nullable File projectFile) {
        if(projectFile != null && projectFile.isFile()){
            super.loadNewProject(project, projectFile);
            if(project instanceof RagdollProject ragdollProject && !RagdollTypeRegistry.getAllType().contains(ragdollProject.file.getType()))
                closeCurrentProject(false, Runnables.doNothing());
            return;
        }
        var dialog = RagdollDialogHelper.createNewRagdollDialog(project, rl -> {
            var file = RagdollReloadListener.LOCAL_DIR.resolve(rl.getNamespace()).resolve(rl.getPath() + ".json");
            super.loadNewProject(project, file.toFile());
        });
        dialog.show(getModularUI());
    }
}
