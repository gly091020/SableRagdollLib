package com.gly091020.SableRagdollLib.editor.api;

import com.gly091020.SableRagdollLib.resource.file.RagdollRenderData;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.ISceneRendering;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.SceneObject;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractModelSceneObject extends SceneObject implements ISceneRendering {

    public final List<RagdollRenderData.EveryPart> parts = new ArrayList<>();

    @Override
    public void drawInternal(PoseStack poseStack, MultiBufferSource bufferSource, float partialTicks) {
        float alpha = 0.1f;
        if(parts.isEmpty())alpha = 1f;
        drawMain(poseStack, bufferSource, partialTicks, alpha);
        drawPart(poseStack, bufferSource, partialTicks);
    }

    public abstract void drawMain(PoseStack poseStack, MultiBufferSource bufferSource, float partialTicks, float alpha);
    public abstract void drawPart(PoseStack poseStack, MultiBufferSource bufferSource, float partialTicks);

    public void setParts(List<RagdollRenderData.EveryPart> parts){
        this.parts.clear();
        this.parts.addAll(parts);
    }

    public void clearParts(){
        parts.clear();
    }
}
