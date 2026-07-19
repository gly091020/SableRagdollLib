package com.gly091020.SableRagdollLib.editor.view;

import com.gly091020.SableRagdollLib.editor.RagdollEditor;
import com.gly091020.SableRagdollLib.editor.project.RagdollProject;
import com.gly091020.SableRagdollLib.editor.sceneObject.PartObject;
import com.gly091020.SableRagdollLib.resource.file.RagdollHitbox;
import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.SceneEditor;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.SceneObject;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.utils.BlockModelObject;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.DummyWorld;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3f;

import java.util.List;

public class PartEditorView extends View {
    public SceneEditor partEditor;
    public final RagdollEditor editor;

    public PartEditorView(RagdollEditor editor){
        this.editor = editor;
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

    public void addBlocks(){
        BlockPos.betweenClosedStream(-1, 0, -1, 1, 0, 1).forEach(
            pos -> {
                var block = new BlockModelObject();
                block.transform().position(new Vector3f(pos.getX(), pos.getY() - 0.5f, pos.getZ()));
                block.blockState = Blocks.GRASS_BLOCK.defaultBlockState();
                partEditor.addSceneObject(block);
            }
        );
    }

    public void clear(){
        partEditor.getAllSceneObjects().clear();
    }

    // ChatGPT 数学比我好多了
    public void reloadParts(RagdollProject project) {
        clear();
        addBlocks();
        var root = new SceneObject();
        root.transform().rotate(
                new Vector3f(0, 1, 0),
                (float) Math.toRadians(180)
        );
        partEditor.addSceneObject(root);
        for (String part : project.file.allParts) {
            var partBox = project.file.hitbox.stream()
                    .filter(e -> e.name.equals(part))
                    .findFirst()
                    .map(e -> e.box)
                    .orElse(null);
            var pos = project.file.position.stream()
                    .filter(e -> e.name.equals(part))
                    .findFirst()
                    .map(e -> e.setting)
                    .orElse(null);
            var renderData = project.file.renderData.stream()
                    .filter(e -> e.name.equals(part))
                    .findFirst()
                    .map(e -> e.data)
                    .orElse(null);
            if (partBox == null || pos == null || renderData == null) {
                continue;
            }
            Vector3f center = getBoxCenter(partBox.toPartBox());
            var o = new PartObject(
                    editor,
                    part,
                    partBox.toPartBox(),
                    renderData.toPartRenderData(),
                    project.file.joints
            );
            o.transform().localPosition(
                    new Vector3f(
                            -center.x,
                            -center.y,
                            -center.z
                    )
            );
            var pivot = new SceneObject();
            pivot.transform().localPosition(
                    new Vector3f(
                            pos.transform.x,
                            pos.transform.y,
                            pos.transform.z
                    ).mul(1 / 16f)
            );
            pivot.transform().rotate(
                    new Vector3f(1,0,0),
                    (float)Math.toRadians(pos.rotation.x)
            );
            pivot.transform().rotate(
                    new Vector3f(0,1,0),
                    (float)Math.toRadians(pos.rotation.y)
            );
            pivot.transform().rotate(
                    new Vector3f(0,0,1),
                    (float)Math.toRadians(pos.rotation.z)
            );
            o.transform().parent(pivot.transform(), false);
            pivot.transform().parent(root.transform(), false);
            partEditor.addSceneObject(pivot);
            partEditor.addSceneObject(o);
        }
    }

    private Vector3f getBoxCenter(RagdollHitbox.PartBox partBox) {

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE;

        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        float maxZ = -Float.MAX_VALUE;


        for (RagdollHitbox.Box box : partBox.boxes()) {

            minX = Math.min(minX, box.minX());
            minY = Math.min(minY, box.minY());
            minZ = Math.min(minZ, box.minZ());

            maxX = Math.max(maxX, box.maxX());
            maxY = Math.max(maxY, box.maxY());
            maxZ = Math.max(maxZ, box.maxZ());
        }


        return new Vector3f(
                (minX + maxX) / 2f,
                (minY + maxY) / 2f,
                (minZ + maxZ) / 2f
        );
    }
}
