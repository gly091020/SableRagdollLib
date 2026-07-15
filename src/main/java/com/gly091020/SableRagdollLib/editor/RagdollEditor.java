package com.gly091020.SableRagdollLib.editor;

import com.gly091020.SableRagdollLib.SableRagdollLib;
import com.gly091020.SableRagdollLib.api.RagdollTypeRegistry;
import com.gly091020.SableRagdollLib.common.RagdollReloadListener;
import com.gly091020.SableRagdollLib.common.ServerGetter;
import com.gly091020.SableRagdollLib.editor.project.RagdollProject;
import com.gly091020.SableRagdollLib.editor.project.RagdollProjectType;
import com.gly091020.SableRagdollLib.editor.sceneObject.PartObject;
import com.gly091020.SableRagdollLib.editor.util.RagdollDialogHelper;
import com.gly091020.SableRagdollLib.editor.util.RagdollFileMenu;
import com.google.common.util.concurrent.Runnables;
import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.SceneEditor;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.utils.BlockModelObject;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.DummyWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class RagdollEditor extends Editor {
    public static final ResourceLocation WINDOW_ID = ResourceLocation.fromNamespaceAndPath(SableRagdollLib.MODID, "editor");
    public RagdollFileMenu ragdollFileMenu;
    public SceneEditor partEditor;

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
        initPartEditorView();
    }

    @Override
    protected void onPrepareResourceView() {

    }

    @Override
    protected void onPrepareHistoryView() {

    }

    public void initPartEditorView(){
        this.partEditor = new SceneEditor();
        partEditor.scene
                .createScene(new DummyWorld())
                .setTickWorld(true)
                .setRenderedCore(List.of(
                        BlockPos.ZERO
                ))
                .useCacheBuffer();
        partEditor.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        var v = new View();
        v.addChild(partEditor);
        placeView(v, () -> centerWindow.getLeftTop());
    }

    public void reloadParts(){
        partEditor.getAllSceneObjects().clear();

        var project = getRagdollProject();
        if(project == null)return;
        for (String part: project.file.getAllParts()){
            var partBox = project.file.getHitbox().hitbox().get(part);
            var pos = project.file.getPosition().position().get(part);
            if(partBox == null || pos == null)return;
            var o = new PartObject(partBox);
            o.transform().position(new Vector3f((float) pos.transform().x,
                            (float) pos.transform().y,
                            (float) pos.transform().z).mul(1 / 16f));
            o.transform().rotate(new Vector3f(1, 0, 0), (float) Math.toRadians(pos.rotation().x));
            o.transform().rotate(new Vector3f(0, 1, 0), (float) Math.toRadians(pos.rotation().y));
            o.transform().rotate(new Vector3f(0, 0, 1), (float) Math.toRadians(pos.rotation().z));
            partEditor.addSceneObject(o);
        }
    }

    public RagdollProject getRagdollProject(){
        return (RagdollProject) getCurrentProject();
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

    @Override
    public void saveProject(@Nullable Runnable onFinish) {
        super.saveProject(onFinish);
        ServerGetter.getServer().ifPresent(server -> RagdollReloadListener.reload(server.getResourceManager()));
    }
}
