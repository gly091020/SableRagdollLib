package com.gly091020.SableRagdollLib.editor;

import com.gly091020.SableRagdollLib.SableRagdollLib;
import com.gly091020.SableRagdollLib.api.RagdollTypeRegistry;
import com.gly091020.SableRagdollLib.common.RagdollReloadListener;
import com.gly091020.SableRagdollLib.common.ServerGetter;
import com.gly091020.SableRagdollLib.editor.project.RagdollProject;
import com.gly091020.SableRagdollLib.editor.project.RagdollProjectType;
import com.gly091020.SableRagdollLib.editor.util.RagdollDialogHelper;
import com.gly091020.SableRagdollLib.editor.util.RagdollFileMenu;
import com.gly091020.SableRagdollLib.editor.view.ModelTreeView;
import com.gly091020.SableRagdollLib.editor.view.ModelView;
import com.gly091020.SableRagdollLib.editor.view.PartEditorView;
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

    public PartEditorView partEditorView;
    public ModelView modelView;
    public ModelTreeView modelTreeView;

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
        initRagdollEditorView();
    }

    @Override
    protected void onPrepareResourceView() {

    }

    @Override
    protected void onPrepareHistoryView() {

    }

    public void initRagdollEditorView(){
        modelView = new ModelView(this);
        partEditorView = new PartEditorView();
        modelTreeView = new ModelTreeView(modelView);
        placeView(modelView, () -> centerWindow.getLeftTop());
        placeView(partEditorView, () -> centerWindow.getLeftTop());
        placeView(modelTreeView, () -> leftWindow.getLeftTop());
    }

    public void reloadParts(){
        var project = getRagdollProject();
        if(project == null)return;
        partEditorView.reloadParts(project);
        modelView.initModel(project);
        modelTreeView.initTree();
    }

    public RagdollProject getRagdollProject(){
        return (RagdollProject) getCurrentProject();
    }

    @Override
    protected void loadNewProject(@NotNull IProject project, @Nullable File projectFile) {
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

    @Override
    public void saveProject(@Nullable Runnable onFinish) {
        super.saveProject(onFinish);
        ServerGetter.getServer().ifPresent(server -> RagdollReloadListener.reload(server.getResourceManager()));
    }
}
