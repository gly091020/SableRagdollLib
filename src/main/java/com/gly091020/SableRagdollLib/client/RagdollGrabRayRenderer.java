package com.gly091020.SableRagdollLib.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 抓取射线渲染：木偶模式下画出"手正方向"的探测射线（命中=黄色，未命中=暗红）
 * 与选中的抓取位置（绿色小方框）。数据来自服务端 {@code ClientboundRagdollGrabRayPacket}。
 */
public final class RagdollGrabRayRenderer {
    private static boolean visible;
    private static Vec3 origin = Vec3.ZERO;
    private static Vec3 target = Vec3.ZERO;
    private static boolean hit;

    private RagdollGrabRayRenderer() {
    }

    /** 服务端推送射线数据；visible=false 表示离开瞄准状态，清除射线。 */
    public static void update(Vec3 originIn, Vec3 targetIn, boolean hitIn, boolean visibleIn) {
        if (!visibleIn) {
            clear();
            return;
        }
        origin = originIn;
        target = targetIn;
        hit = hitIn;
        visible = true;
    }

    /** 停止控制时清除射线。 */
    public static void clear() {
        visible = false;
    }

    /** 在世界渲染阶段调用（RenderLevelStageEvent）。
     *  注意：这个版本事件的 poseStack 需要手动减去相机位置（与 NeoForge 自带调试渲染器同一套），
     *  直接画世界坐标会整个偏出屏幕。 */
    public static void render(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 camera) {
        if (!visible) {
            return;
        }
        VertexConsumer lines = bufferSource.getBuffer(RenderType.lines());
        poseStack.pushPose();
        // 世界坐标 → 相机相对坐标
        poseStack.translate(origin.x - camera.x, origin.y - camera.y, origin.z - camera.z);
        Vec3 localTarget = target.subtract(origin);
        // 射线：命中=黄色，未命中=暗红
        drawLine(poseStack, lines, Vec3.ZERO, localTarget, hit ? 1f : 0.6f, hit ? 1f : 0.2f, hit ? 0.2f : 0.2f);
        if (hit) {
            // 选中的抓取位置：绿色小方框
            LevelRenderer.renderLineBox(
                    poseStack, bufferSource.getBuffer(RenderType.lines()),
                    new AABB(localTarget, localTarget).inflate(0.15),
                    0f, 1f, 0f, 1f);
        }
        poseStack.popPose();
    }

    private static void drawLine(PoseStack poseStack, VertexConsumer vc, Vec3 p1, Vec3 p2, float r, float g, float b) {
        var mat = poseStack.last().pose();
        vc.addVertex(mat, (float) p1.x, (float) p1.y, (float) p1.z)
                .setColor(r, g, b, 1f)
                .setNormal(0f, 1f, 0f);
        vc.addVertex(mat, (float) p2.x, (float) p2.y, (float) p2.z)
                .setColor(r, g, b, 1f)
                .setNormal(0f, 1f, 0f);
    }
}