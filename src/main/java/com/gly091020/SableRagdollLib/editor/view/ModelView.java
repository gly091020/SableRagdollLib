package com.gly091020.SableRagdollLib.editor.view;

import com.gly091020.SableRagdollLib.editor.RagdollEditor;
import com.gly091020.SableRagdollLib.editor.api.AbstractModelSceneObject;
import com.gly091020.SableRagdollLib.editor.api.ModelSceneManager;
import com.gly091020.SableRagdollLib.editor.project.RagdollProject;
import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.SceneEditor;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.DummyWorld;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ModelView extends View {
    public SceneEditor modelEditor;
    public final RagdollEditor editor;
    private AbstractModelSceneObject sceneObject;

    public ModelView(RagdollEditor editor) {
        this.editor = editor;
        setActive(false);
        this.modelEditor = new SceneEditor();
        modelEditor.scene
                .createScene(new DummyWorld())
                .setTickWorld(true)
                .setRenderedCore(List.of(
                        BlockPos.ZERO
                ))
                .useCacheBuffer();
        modelEditor.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        addChild(modelEditor);
    }

    public void clear(){
        modelEditor.getAllSceneObjects().clear();
        sceneObject = null;
    }

    public void initModel(RagdollProject project){
        clear();
        setActive(false);
        if(editor.getCurrentProjectFile() == null)return;
        var supplier = ModelSceneManager.get(project.file.type);
        if(supplier == null)return;
        var modelID = supplier.getModel(editor.getCurrentProjectFile().toPath());
        if(modelID == null)return;
        var o = supplier.createNewModelObject(modelID);
        modelEditor.addSceneObject(o);
        sceneObject = o;
        setActive(true);
    }

    @Nullable
    public AbstractModelSceneObject getSceneObject() {
        return sceneObject;
    }
}
