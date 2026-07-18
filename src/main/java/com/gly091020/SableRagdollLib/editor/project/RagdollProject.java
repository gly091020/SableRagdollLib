package com.gly091020.SableRagdollLib.editor.project;

import com.gly091020.SableRagdollLib.editor.RagdollEditor;
import com.gly091020.SableRagdollLib.resource.editor.EditorDefFile;
import com.gly091020.SableRagdollLib.resource.file.RagdollDefFile;
import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.editor.project.ProjectType;
import com.lowdragmc.lowdraglib2.editor.resource.Resources;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public class RagdollProject implements IProject {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Resources resources = Resources.of();

    public EditorDefFile file;
    @Override
    public Resources getResources() {
        return resources;
    }

    @Override
    public ProjectType getProjectType() {
        return RagdollProjectType.TYPE;
    }

    @Override
    public void initNewProject() {
        file = EditorDefFile.createEmpty();
    }

    @Override
    public CompoundTag serializeProject(@NotNull HolderLookup.Provider provider) {
        if(file == null)return new CompoundTag();
        var r = RagdollDefFile.CODEC.encodeStart(NbtOps.INSTANCE, file.toRecord())
                .resultOrPartial(e -> LOGGER.error("保存时出现错误：{}", e));
        return r.map(tag -> (CompoundTag) tag).orElseGet(CompoundTag::new);
    }

    @Override
    public void deserializeProject(@NotNull HolderLookup.Provider provider, @NotNull CompoundTag nbt) {
        RagdollDefFile.CODEC.parse(NbtOps.INSTANCE, nbt)
                .resultOrPartial(e -> LOGGER.error("加载时出现错误：{}", e))
                .ifPresent(defFile -> file = EditorDefFile.fromRecord(defFile));
    }

    @Override
    public void onLoad(Editor editor) {
        if(editor instanceof RagdollEditor ragdollEditor) {
            ragdollEditor.reloadParts();
            ragdollEditor.initInspector();
        }
    }
}
