package com.gly091020.SableRagdollLib.editor.project;

import com.gly091020.SableRagdollLib.resource.editor.EditorDefFile;
import com.gly091020.SableRagdollLib.resource.file.RagdollDefFile;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.editor.project.ProjectType;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.function.Supplier;

public class RagdollProjectType extends ProjectType {
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    public static final RagdollProjectType TYPE = new RagdollProjectType(
            Icons.FILE,
            Component.translatable("text.sableragdolllib.ragdoll").getString(),
            ".json",
            RagdollProject::new
    );
    private static final Logger LOGGER = LogUtils.getLogger();

    private RagdollProjectType(IGuiTexture icon, String name, String suffix, Supplier<IProject> projectCreator) {
        super(icon, name, suffix, projectCreator);
    }

    @Override
    public IProject loadProjectFromFile(File file) throws Exception {
        var instance = new RagdollProject();
        var p = RagdollDefFile.CODEC.parse(JsonOps.INSTANCE,
                        GSON.fromJson(Files.readString(file.toPath()),
                                JsonElement.class))
                .resultOrPartial(e -> LOGGER.error("加载时出现错误：{}", e));
        p.ifPresent(defFile -> instance.file = EditorDefFile.fromRecord(defFile));
        return instance;
    }

    @Override
    public void saveProjectToFile(IProject project, File file) throws Exception {
        if(!(project instanceof RagdollProject rp))throw new RuntimeException();
        var r = RagdollDefFile.CODEC.encodeStart(JsonOps.INSTANCE, rp.file.toRecord())
                .resultOrPartial(e -> LOGGER.error("保存时出现错误：{}", e));
        if(r.isPresent()) {
            Files.createDirectories(file.toPath().getParent());
            Files.writeString(file.toPath(), GSON.toJson(r.get()), StandardOpenOption.CREATE);
        }
    }

    @Override
    public boolean isProjectDirty(IProject project, File file) throws Exception {
        if(!(project instanceof RagdollProject rp))return false;
        var r = RagdollDefFile.CODEC.encodeStart(JsonOps.INSTANCE, rp.file.toRecord())
                .resultOrPartial(e -> LOGGER.error("读取时出现错误：{}", e));
        if(r.isEmpty())return true;
        return !GSON.toJson(r.get()).equals(Files.readString(file.toPath()));
    }
}
