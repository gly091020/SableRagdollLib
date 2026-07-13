package com.gly091020.SableRagdollLib.client.renderer;

import com.gly091020.SableRagdollLib.entity.PartSeat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class PartSeatRenderer extends EntityRenderer<PartSeat> {

    public PartSeatRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(
            PartSeat entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {

    }

    @Override
    public ResourceLocation getTextureLocation(PartSeat entity) {
        return ResourceLocation.withDefaultNamespace("missing");
    }
}