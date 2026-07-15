package com.gly091020.SableRagdollLib.editor.sceneObject;

import com.gly091020.SableRagdollLib.resource.file.RagdollHitbox;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.ISceneInteractable;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.ISceneRendering;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.SceneObject;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PartObject extends SceneObject implements ISceneRendering, ISceneInteractable {
    public RagdollHitbox.PartBox partBox;
    public PartObject(RagdollHitbox.PartBox partBox){
        this.partBox = partBox;
    }

    @Override
    public void drawInternal(PoseStack poseStack, MultiBufferSource bufferSource, float partialTicks) {
        for (RagdollHitbox.Box box: partBox.boxes()){
            drawLineBox(poseStack, bufferSource,
                    new Vec3(box.minX(), box.minY(), box.minZ()),
                    new Vec3(box.maxX(), box.maxY(), box.maxZ()),
                    1, 1, 1);
        }
    }

    @Override
    public VoxelShape getCollisionShape() {
        return partBox.toVoxelShape();
    }

    public void setPartBox(RagdollHitbox.PartBox partBox) {
        this.partBox = partBox;
    }

    public void drawLineBox(PoseStack poseStack, MultiBufferSource multiBufferSource, Vec3 pos1, Vec3 pos2, int r, int g, int b){
        LevelRenderer.renderLineBox(
                poseStack,
                multiBufferSource.getBuffer(RenderType.lines()),
                new AABB(pos1, pos2),
                r, g, b, 1
        );
    }
}
