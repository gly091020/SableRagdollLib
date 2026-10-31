package com.gly091020.SableRagdollLib.client;

import com.gly091020.SableRagdollLib.api.control.RagdollControlDebug;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 本地集成服务器下，按 F3 显示布娃娃落脚点与脚底探测结果。
 */
public final class RagdollControlDebugRenderer {
    private RagdollControlDebugRenderer() {
    }

    public static void render(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 camera) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        var snapshots = RagdollControlDebug.snapshots();
        if (snapshots.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        VertexConsumer lines = bufferSource.getBuffer(RenderType.lines());
        for (var snapshot : snapshots) {
            if (!snapshot.dimension().equals(level.dimension())
                    || Math.abs(level.getGameTime() - snapshot.gameTime()) > 20) {
                continue;
            }
            if (snapshot.stanceFoothold() != null) {
                box(poseStack, bufferSource, snapshot.stanceFoothold(), 0.14, 1, 1, 0);
            }
            if (snapshot.nextFoothold() != null) {
                box(poseStack, bufferSource, snapshot.nextFoothold(), 0.11, 0, 1, 1);
            }
            if (snapshot.leftFoot() != null) {
                box(poseStack, bufferSource, snapshot.leftFoot(), 0.08, 0.35f, 0.65f, 1);
            }
            if (snapshot.rightFoot() != null) {
                box(poseStack, bufferSource, snapshot.rightFoot(), 0.08, 1, 0.55f, 0.15f);
            }
            if (snapshot.footRayOrigin() != null && snapshot.footRayEnd() != null) {
                float red = snapshot.footRayHit() ? 0.1f : 1;
                float green = snapshot.footRayHit() ? 1 : 0.15f;
                line(poseStack, lines, snapshot.footRayOrigin(), snapshot.footRayEnd(), red, green, 0.15f);
            }
            if (snapshot.bodyAnchor() != null && snapshot.bodyTarget() != null) {
                line(poseStack, lines, snapshot.bodyAnchor(), snapshot.bodyTarget(), 1, 0.2f, 1);
            }
        }
        poseStack.popPose();
    }

    private static void box(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 pos, double radius,
                            float red, float green, float blue) {
        LevelRenderer.renderLineBox(
                poseStack,
                bufferSource.getBuffer(RenderType.lines()),
                new AABB(pos, pos).inflate(radius),
                red, green, blue, 1
        );
    }

    private static void line(PoseStack poseStack, VertexConsumer consumer, Vec3 from, Vec3 to,
                             float red, float green, float blue) {
        var pose = poseStack.last().pose();
        consumer.addVertex(pose, (float) from.x, (float) from.y, (float) from.z)
                .setColor(red, green, blue, 1)
                .setNormal(0, 1, 0);
        consumer.addVertex(pose, (float) to.x, (float) to.y, (float) to.z)
                .setColor(red, green, blue, 1)
                .setNormal(0, 1, 0);
    }
}
