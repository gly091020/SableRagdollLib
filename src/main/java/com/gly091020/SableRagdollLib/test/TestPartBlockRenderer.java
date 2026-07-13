package com.gly091020.SableRagdollLib.test;

import com.gly091020.SableRagdollLib.client.renderer.AbstractPartBlockRenderer;
import com.gly091020.SableRagdollLib.resource.file.RagdollRenderData;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.HashMap;
import java.util.Map;

public class TestPartBlockRenderer extends AbstractPartBlockRenderer<TestPartBlockEntity> {
    private final BlockEntityRendererProvider.Context context;
    private final Map<String, Block> blockMap = new HashMap<>();
    public TestPartBlockRenderer(BlockEntityRendererProvider.Context context){
        this.context = context;
    }

    @Override
    public void renderMain(TestPartBlockEntity blockEntity, float delta, PoseStack poseStack, MultiBufferSource multiBufferSource, int light, int overlay) {
        drawLineBox(poseStack, multiBufferSource, blockEntity, 1, 1, 1);
        var renderData = blockEntity.getPartData().renderData();
        for(RagdollRenderData.EveryPart part: renderData.parts()){
            BlockState blockState;
            if(blockMap.containsKey(part.partName()))
                blockState = blockMap.get(part.partName()).defaultBlockState();
            else {
                blockState = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(part.partName())).orElse(Blocks.AIR).defaultBlockState();
                blockMap.put(part.partName(), blockState.getBlock());
            }
            context.getBlockRenderDispatcher().renderSingleBlock(blockState, poseStack, multiBufferSource, light, overlay, ModelData.EMPTY, null);
        }
    }

    @Override
    public void transformBefore(TestPartBlockEntity blockEntity, PoseStack poseStack) {

    }

    private void drawLineBox(PoseStack poseStack, MultiBufferSource multiBufferSource, TestPartBlockEntity blockEntity, int r, int g, int b){
        LevelRenderer.renderLineBox(
                poseStack,
                multiBufferSource.getBuffer(RenderType.lines()),
                blockEntity.getShape().bounds(),
                r, g, b, 1
        );
    }
}
