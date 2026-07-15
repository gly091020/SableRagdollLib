package com.gly091020.SableRagdollLib.editor.view;

import com.gly091020.SableRagdollLib.editor.project.RagdollProject;
import com.gly091020.SableRagdollLib.editor.sceneObject.PartObject;
import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.SceneEditor;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.DummyWorld;
import net.minecraft.core.BlockPos;
import org.joml.Vector3f;

import java.util.List;

public class PartEditorView extends View {
    public SceneEditor partEditor;

    public PartEditorView(){
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
        addChild(partEditor);
    }

    public void clear(){
        partEditor.getAllSceneObjects().clear();
    }

    public void reloadParts(RagdollProject project){
        clear();
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
}
