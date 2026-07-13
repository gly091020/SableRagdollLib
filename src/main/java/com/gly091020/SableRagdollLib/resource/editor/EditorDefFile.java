package com.gly091020.SableRagdollLib.resource.editor;

import com.gly091020.SableRagdollLib.SableRagdollLib;
import com.gly091020.SableRagdollLib.resource.file.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EditorDefFile {
    private ResourceLocation type;
    private List<String> allParts;
    private RagdollHitbox hitbox;
    private RagdollPosition position;
    private RagdollRenderData renderData;
    private RagdollJoints joints;
    private String mainBody;
    private CompoundTag extra;

    public EditorDefFile(
            ResourceLocation type,
            List<String> allParts,
            RagdollHitbox hitbox,
            RagdollPosition position,
            RagdollRenderData renderData,
            RagdollJoints joints,
            String mainBody,
            CompoundTag extra
    ) {
        this.type = type;
        this.allParts = allParts;
        this.hitbox = hitbox;
        this.position = position;
        this.renderData = renderData;
        this.joints = joints;
        this.mainBody = mainBody;
        this.extra = extra;
    }

    public ResourceLocation getType() {
        return type;
    }

    public void setType(ResourceLocation type) {
        this.type = type;
    }

    public List<String> getAllParts() {
        return allParts;
    }

    public void setAllParts(List<String> allParts) {
        this.allParts = allParts;
    }

    public RagdollHitbox getHitbox() {
        return hitbox;
    }

    public void setHitbox(RagdollHitbox hitbox) {
        this.hitbox = hitbox;
    }

    public RagdollPosition getPosition() {
        return position;
    }

    public void setPosition(RagdollPosition position) {
        this.position = position;
    }

    public RagdollRenderData getRenderData() {
        return renderData;
    }

    public void setRenderData(RagdollRenderData renderData) {
        this.renderData = renderData;
    }

    public RagdollJoints getJoints() {
        return joints;
    }

    public void setJoints(RagdollJoints joints) {
        this.joints = joints;
    }

    public Optional<String> getMainBody() {
        return Optional.ofNullable(mainBody);
    }

    public void setMainBody(String mainBody) {
        this.mainBody = mainBody;
    }

    public CompoundTag getExtra() {
        return extra;
    }

    public void setExtra(CompoundTag extra) {
        this.extra = extra;
    }

    public RagdollDefFile toRecord(){
        return new RagdollDefFile(
                type, allParts, hitbox, position, renderData, joints, Optional.ofNullable(mainBody), extra
        );
    }

    public static EditorDefFile fromRecord(RagdollDefFile defFile){
        return new EditorDefFile(defFile.type(), defFile.allParts(), defFile.hitbox(), defFile.position(), defFile.renderData(), defFile.joints(), defFile.mainBody().orElse(null), defFile.extra());
    }

    public static final ResourceLocation EMPTY = ResourceLocation.fromNamespaceAndPath(SableRagdollLib.MODID, "empty");

    public static EditorDefFile createEmpty(){
        return new EditorDefFile(
                EMPTY, List.of(),
                new RagdollHitbox(Map.of()),
                new RagdollPosition(Map.of()),
                new RagdollRenderData(Map.of()),
                new RagdollJoints(List.of()),
                null,
                new CompoundTag()
        );
    }
}