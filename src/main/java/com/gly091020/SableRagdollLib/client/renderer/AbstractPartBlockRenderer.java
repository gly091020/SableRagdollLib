package com.gly091020.SableRagdollLib.client.renderer;

import com.gly091020.SableRagdollLib.block.AbstractPartBlockEntity;
import com.gly091020.SableRagdollLib.command.SableRagdollLibClientCommand;
import com.gly091020.SableRagdollLib.common.DefFileLoader;
import com.gly091020.SableRagdollLib.resource.file.RagdollHitbox;
import com.gly091020.SableRagdollLib.resource.file.RagdollJoints;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.Objects;

public abstract class AbstractPartBlockRenderer<T extends AbstractPartBlockEntity> implements BlockEntityRenderer<T> {
    @Override
    public final void render(T blockEntity, float v, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, int i1) {
        if(blockEntity.getPartData() == null)return;
        if(SableRagdollLibClientCommand.debugRender) {
            renderDebug(blockEntity, poseStack, multiBufferSource);
            return;
        }
        firstRender(blockEntity, v, poseStack, multiBufferSource, i, i1);
        poseStack.pushPose();
        transformBefore(blockEntity, poseStack);
        transformUser(blockEntity, poseStack);
        renderMain(blockEntity, v, poseStack, multiBufferSource, i, i1);
        poseStack.popPose();
    }

    public final void transformUser(T blockEntity, PoseStack poseStack){
        var data = blockEntity.getPartData();
        var transform = data.renderData().transform();
        var rotate = data.renderData().rotation();
        var scale = data.renderData().scale();
        poseStack.translate(transform.x / 16, transform.y / 16, transform.z / 16);
        poseStack.mulPose(new Quaternionf().rotateXYZ((float) Math.toRadians(rotate.x),
                (float) Math.toRadians(rotate.y),
                (float) Math.toRadians(rotate.z)));
        poseStack.scale((float) scale.x, (float) scale.y, (float) scale.z);
    }

    public void firstRender(T blockEntity, float v, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, int i1){};

    public abstract void renderMain(T blockEntity, float delta, PoseStack poseStack, MultiBufferSource multiBufferSource, int light, int overlay);

    public abstract void transformBefore(T blockEntity, PoseStack poseStack);

    public void renderDebug(T blockEntity, PoseStack poseStack, MultiBufferSource multiBufferSource){
        for (RagdollHitbox.Box box: blockEntity.getPartData().hitbox().boxes()){
            drawLineBox(poseStack, multiBufferSource,
                    new Vec3(box.minX(), box.minY(), box.minZ()),
                    new Vec3(box.maxX(), box.maxY(), box.maxZ()),
                    1, 1, 1);
        }
        var data = DefFileLoader.getDefFile(blockEntity.getPartData().defFile());
        if(data == null)return;
        var vc = multiBufferSource.getBuffer(RenderType.lines());
        final double size = 0.1;
        for (RagdollJoints.JointData jointData: data.joints().jointData()){
            if(Objects.equals(jointData.a(), blockEntity.getPartData().partName())){
                var pos = jointData.posA().scale(1 / 16f).add(0.5, 0.5, 0.5);
                drawLine(poseStack, vc, pos.subtract(0, -size, 0),
                        pos.subtract(0, size, 0),
                        1, 0, 0);
                drawLine(poseStack, vc, pos.subtract(0, 0, -size),
                        pos.subtract(0, 0, size),
                        1, 0, 0);
                drawLine(poseStack, vc, pos.subtract(-size, 0, 0),
                        pos.subtract(size, 0, 0),
                        1, 0, 0);
            }
            if(Objects.equals(jointData.b(), blockEntity.getPartData().partName())){
                var pos = jointData.posB().scale(1 / 16f).add(0.5, 0.5, 0.5);
                drawLine(poseStack, vc, pos.subtract(0, -size, 0),
                        pos.subtract(0, size, 0),
                        0, 0, 1);
                drawLine(poseStack, vc, pos.subtract(0, 0, -size),
                        pos.subtract(0, 0, size),
                        0, 0, 1);
                drawLine(poseStack, vc, pos.subtract(-size, 0, 0),
                        pos.subtract(size, 0, 0),
                        0, 0, 1);
            }
        }
    }

    public void drawLineBox(PoseStack poseStack, MultiBufferSource multiBufferSource, Vec3 pos1, Vec3 pos2, int r, int g, int b){
        LevelRenderer.renderLineBox(
                poseStack,
                multiBufferSource.getBuffer(RenderType.lines()),
                new AABB(pos1, pos2),
                r, g, b, 1
        );
    }

    public void drawLine(PoseStack poseStack, VertexConsumer vc, Vec3 pos1, Vec3 pos2, float r, float g, float b) {
        var mat = poseStack.last().pose();
        vc.addVertex(mat, (float)pos1.x, (float)pos1.y, (float)pos1.z)
                .setColor(r, g, b, 1f)
                .setNormal(0f, 1f, 0f);

        vc.addVertex(mat, (float)pos2.x, (float)pos2.y, (float)pos2.z)
                .setColor(r, g, b, 1f)
                .setNormal(0f, 1f, 0f);
    }
}
