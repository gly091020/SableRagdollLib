package com.gly091020.SableRagdollLib.editor.sceneObject;

import com.gly091020.SableRagdollLib.api.RagdollTypeRegistry;
import com.gly091020.SableRagdollLib.block.AbstractPartBlockEntity;
import com.gly091020.SableRagdollLib.editor.RagdollEditor;
import com.gly091020.SableRagdollLib.resource.editor.EditorRagdollJoints;
import com.gly091020.SableRagdollLib.resource.file.RagdollExpressions;
import com.gly091020.SableRagdollLib.resource.file.RagdollHitbox;
import com.gly091020.SableRagdollLib.resource.file.RagdollRenderData;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.ISceneInteractable;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.ISceneRendering;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.SceneObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3d;

import java.nio.file.Path;
import java.util.*;

public class PartObject extends SceneObject implements ISceneRendering, ISceneInteractable {
    public final String partName;
    public RagdollHitbox.PartBox partBox;
    public RagdollEditor editor;
    public RagdollRenderData.PartRenderData renderData;
    public final List<EditorRagdollJoints.EditorJointData> jointData;

    public AbstractPartBlockEntity fakeBE;
    public BlockEntityRenderer<AbstractPartBlockEntity> renderer;
    public PartObject(RagdollEditor editor, String partName, RagdollHitbox.PartBox partBox, RagdollRenderData.PartRenderData partRenderData, List<EditorRagdollJoints.EditorJointData> jointData){
        this.partBox = partBox;
        this.renderData = partRenderData;
        this.editor = editor;
        this.jointData = jointData;
        this.partName = partName;

        var type = RagdollTypeRegistry.getRagdollType(editor.getRagdollProject().file.type);
        if(type == null)return;
        fakeBE = type.partBE().get().create(BlockPos.ZERO, type.partBlock().get().defaultBlockState());
        if(fakeBE == null)return;
        var project = editor.getRagdollProject();
        if(editor.getCurrentProjectFile() == null)return;
        var modelID = fromProjectFile(editor.getCurrentProjectFile().toPath());
        if(modelID == null)return;
        fakeBE.setData(new AbstractPartBlockEntity.Data(
                false, partName, UUID.randomUUID(), modelID, project.file.type, partBox, renderData, Optional.empty(), new RagdollExpressions(Map.of()), List.of()
        ));
        renderer = Minecraft.getInstance()
                .getBlockEntityRenderDispatcher()
                .getRenderer(fakeBE);
    }

    @Override
    public void drawInternal(PoseStack poseStack, MultiBufferSource bufferSource, float partialTicks) {
        switch (editor.editorRenderMode){
            case HITBOX -> drawLine(poseStack, bufferSource, partialTicks);
            case RENDERER -> drawRenderData(poseStack, bufferSource, partialTicks);
            case JOINT -> drawJoints(poseStack, bufferSource, partialTicks);
        }
    }

    public void drawLine(PoseStack poseStack, MultiBufferSource bufferSource, float partialTicks){
        for (RagdollHitbox.Box box: partBox.boxes()){
            drawLineBox(poseStack, bufferSource,
                    new Vec3(box.minX(), box.minY(), box.minZ()),
                    new Vec3(box.maxX(), box.maxY(), box.maxZ()),
                    1, 1, 1);
        }
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

    public void drawRenderData(PoseStack poseStack, MultiBufferSource bufferSource, float partialTicks){
        drawLine(poseStack, bufferSource, partialTicks);
        if(renderer == null)return;
        renderer.render(fakeBE, partialTicks, poseStack, bufferSource, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
    }

    public void drawJoints(PoseStack poseStack, MultiBufferSource bufferSource, float partialTicks){
        drawLine(poseStack, bufferSource, partialTicks);
        var vc = bufferSource.getBuffer(RenderType.lines());
        final double size = 0.1;
        for (EditorRagdollJoints.EditorJointData jointData: jointData){
            Vec3 pos1 = null;
            Vec3 pos2 = null;
            if(Objects.equals(jointData.a, partName)){
                pos1 = JOMLConversion.toMojang(new Vector3d(jointData.posA)).scale(1 / 16f).add(0.5, 0.5, 0.5);
                drawLine(poseStack, vc, pos1.subtract(0, -size, 0),
                        pos1.subtract(0, size, 0),
                        1, 0, 0);
                drawLine(poseStack, vc, pos1.subtract(0, 0, -size),
                        pos1.subtract(0, 0, size),
                        1, 0, 0);
                drawLine(poseStack, vc, pos1.subtract(-size, 0, 0),
                        pos1.subtract(size, 0, 0),
                        1, 0, 0);
            }
            if(Objects.equals(jointData.b, partName)){
                pos2 = JOMLConversion.toMojang(new Vector3d(jointData.posB)).scale(1 / 16f).add(0.5, 0.5, 0.5);
                drawLine(poseStack, vc, pos2.subtract(0, -size, 0),
                        pos2.subtract(0, size, 0),
                        0, 0, 1);
                drawLine(poseStack, vc, pos2.subtract(0, 0, -size),
                        pos2.subtract(0, 0, size),
                        0, 0, 1);
                drawLine(poseStack, vc, pos2.subtract(-size, 0, 0),
                        pos2.subtract(size, 0, 0),
                        0, 0, 1);
            }
        }
    }

    public static ResourceLocation fromProjectFile(Path filePath) {
        Path path = filePath.toAbsolutePath().normalize();
        Path root = null;
        Path current = path.getParent();
        while (current != null) {
            if (current.getFileName().toString().equals("sable_ragdoll_lib")) {
                root = current;
                break;
            }
            current = current.getParent();
        }
        if (root == null) {
            return null;
        }
        Path relative = root.relativize(path.getParent());
        if (relative.getNameCount() < 2) {
            return null;
        }
        String namespace = relative.getName(0).toString();
        String resourcePath = root
                .relativize(path)
                .subpath(1, root.relativize(path).getNameCount())
                .toString()
                .replace('\\', '/');
        if (resourcePath.endsWith(".json")) {
            resourcePath = resourcePath.substring(
                    0,
                    resourcePath.length() - 5
            );
        }
        return ResourceLocation.fromNamespaceAndPath(
                namespace,
                resourcePath
        );
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
