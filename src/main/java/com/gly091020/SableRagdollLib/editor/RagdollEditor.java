package com.gly091020.SableRagdollLib.editor;

import com.gly091020.SableRagdollLib.SableRagdollLib;
import com.gly091020.SableRagdollLib.api.RagdollTypeRegistry;
import com.gly091020.SableRagdollLib.common.DefFileLoader;
import com.gly091020.SableRagdollLib.common.RagdollReloadListener;
import com.gly091020.SableRagdollLib.common.ServerGetter;
import com.gly091020.SableRagdollLib.editor.project.RagdollProject;
import com.gly091020.SableRagdollLib.editor.project.RagdollProjectType;
import com.gly091020.SableRagdollLib.editor.util.EditorCommands;
import com.gly091020.SableRagdollLib.editor.util.EditorRenderMode;
import com.gly091020.SableRagdollLib.editor.util.RagdollDialogHelper;
import com.gly091020.SableRagdollLib.editor.util.RagdollFileMenu;
import com.gly091020.SableRagdollLib.editor.view.ModelTreeView;
import com.gly091020.SableRagdollLib.editor.view.ModelView;
import com.gly091020.SableRagdollLib.editor.view.PartEditorView;
import com.gly091020.SableRagdollLib.resource.file.RagdollDefFile;
import com.google.common.util.concurrent.Runnables;
import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class RagdollEditor extends Editor {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation WINDOW_ID = ResourceLocation.fromNamespaceAndPath(SableRagdollLib.MODID, "editor");
    public RagdollFileMenu ragdollFileMenu;

    public PartEditorView partEditorView;
    public ModelView modelView;
    public ModelTreeView modelTreeView;

    public EditorRenderMode editorRenderMode = EditorRenderMode.HITBOX;

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
        initBottomBar();
    }

    @Override
    protected void onPrepareResourceView() {

    }

    public void initBottomBar(){
        var bar = new UIElement();
        var barLeft = new UIElement();
        var barRight = new UIElement();
        barLeft.getLayout().flexDirection(FlexDirection.ROW).flex(1);
        barRight.getLayout().flexDirection(FlexDirection.ROW_REVERSE).flex(1);

        bar.addClass("__view-container__");
        bar.getLayout().height(15).flexDirection(FlexDirection.ROW);
        bar.addChildren(barLeft, barRight);

        for (EditorRenderMode mode: EditorRenderMode.values()){
            var button = new Button();
            button.setText(mode.getName());
            button.setOnClick(event -> editorRenderMode = mode);
            barRight.addChild(button);
        }

        for (EditorCommands editorCommands: EditorCommands.values()){
            var button = new Button();
            button.setText(editorCommands.getName());
            button.setOnClick(event -> editorCommands.run(this));
            barLeft.addChild(button);
        }

        addChild(bar);
    }

    public void initRagdollEditorView(){
        modelView = new ModelView(this);
        partEditorView = new PartEditorView(this);
        modelTreeView = new ModelTreeView(modelView);

        modelView.setName(Component.translatable("text.sableragdolllib.view.model").getString());
        partEditorView.setName(Component.translatable("text.sableragdolllib.view.part_editor").getString());
        modelTreeView.setName(Component.translatable("text.sableragdolllib.view.model_tree").getString());

        placeView(modelView, () -> centerWindow.getLeftTop());
        placeView(partEditorView, () -> centerWindow.getLeftTop());
        placeView(modelTreeView, () -> leftWindow.getLeftTop());
    }

    public void reloadParts(){
        var project = getRagdollProject();
        if(project == null)return;
        reloadModelView();
        modelView.initModel(project);
        modelTreeView.initTree();
    }

    public void initInspector(){
        inspectorView.inspect(getRagdollProject().file, configurator -> reloadModelView());
    }

    public void reloadModelView(){
        partEditorView.reloadParts(getRagdollProject());
    }

    public RagdollProject getRagdollProject(){
        return (RagdollProject) getCurrentProject();
    }

    @Override
    protected void loadNewProject(@NotNull IProject project, @Nullable File projectFile) {
        if(projectFile != null && projectFile.isFile()){
            super.loadNewProject(project, projectFile);
            if(project instanceof RagdollProject ragdollProject && !RagdollTypeRegistry.getAllType().contains(ragdollProject.file.type))
                closeCurrentProject(false, Runnables.doNothing());
            return;
        }

        var dialog = RagdollDialogHelper.createNewRagdollDialog(project, rl -> {
            var file = RagdollReloadListener.LOCAL_DIR.resolve(rl.getFirst().getNamespace()).resolve(rl.getFirst().getPath() + ".json");
            if(rl.getSecond() != null){
                var d = DefFileLoader.getDefFile(rl.getSecond());
                if(d == null)return;
                try {
                    Files.createDirectories(file.getParent());
                    Files.writeString(file,
                            RagdollProjectType.GSON.toJson(RagdollDefFile.CODEC.encodeStart(JsonOps.INSTANCE, d).getOrThrow()),
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    super.loadNewProject(RagdollProjectType.TYPE.loadProjectFromFile(file.toFile()), file.toFile());
                    return;
                } catch (Exception exception) {
                    LOGGER.error("出现错误:", exception);
                }
            }
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
