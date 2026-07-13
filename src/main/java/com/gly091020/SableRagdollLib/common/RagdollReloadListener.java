package com.gly091020.SableRagdollLib.common;

import com.gly091020.SableRagdollLib.command.SableRagdollLibCommand;
import com.gly091020.SableRagdollLib.resource.file.RagdollDefFile;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class RagdollReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogUtils.getLogger();
    public RagdollReloadListener() {
        super(GSON, "ragdoll");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceLocationJsonElementMap, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        LOGGER.info("开始加载文件");
        profilerFiller.push("load_ragdoll");

        DefFileLoader.clear();
        resourceLocationJsonElementMap.forEach(((resourceLocation, jsonElement) ->
                RagdollDefFile.CODEC.parse(JsonOps.INSTANCE, jsonElement)
                .resultOrPartial(e -> LOGGER.error("读取文件时出现错误：{}", e))
                .ifPresent(defFile -> DefFileLoader.add(resourceLocation, defFile))));
        loadLocal();

        profilerFiller.popPush("load_ragdoll");
        LOGGER.info("已完成{}文件的加载", resourceLocationJsonElementMap.size());
    }

    public static void reload(ResourceManager resourceManager){
        DefFileLoader.clear();
        resourceManager.listResources("ragdoll", r -> r.getPath().endsWith(".json")).forEach(
                (resourceLocation, resource) -> {
                    try (Reader reader = resource.openAsReader()) {
                        var json = GSON.fromJson(reader, JsonElement.class);
                        RagdollDefFile.CODEC.parse(JsonOps.INSTANCE, json)
                                .resultOrPartial(e -> LOGGER.error("重载文件时出现错误：{}", e))
                                .ifPresent(defFile -> DefFileLoader.add(
                                        ResourceLocation.fromNamespaceAndPath(resourceLocation.getNamespace(),
                                                resourceLocation.getPath().replace("ragdoll/", "").replace(".json", "")),
                                        defFile));
                    } catch (Exception e) {
                        LOGGER.error("重载文件时出现错误：", e);
                    }
                }
        );
        loadLocal();
    }

    public static final Path LOCAL_DIR = FMLPaths.GAMEDIR.get().resolve(SableRagdollLibCommand.COMMAND);

    public static void loadLocal(){
        if(!LOCAL_DIR.toFile().isDirectory())return;
        try (var stream = Files.list(LOCAL_DIR)) {
            stream.forEach(path -> {
                if(!path.toFile().isDirectory())return;
                loadChild(path);
            });
        } catch (IOException e) {
            LOGGER.error("读取文件夹时出现错误：", e);
        }
    }

    private static void loadChild(Path dir) {
        var namespace = dir.getFileName().toString();

        try (var stream = Files.walk(dir)) {
            stream.forEach(path -> {
                if (!Files.isRegularFile(path)) return;

                var fileName = path.getFileName().toString();
                if (!fileName.endsWith(".json")) return;

                Path relative = dir.relativize(path);

                String idPath = relative.toString()
                        .replace('\\', '/') // Windows 兼容
                        .replaceAll("\\.json$", "");

                JsonElement json;
                try {
                    json = GSON.fromJson(Files.readString(path), JsonElement.class);
                } catch (IOException e) {
                    LOGGER.error("读取文件时出现错误：", e);
                    return;
                }

                RagdollDefFile.CODEC.parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(e -> LOGGER.error("重载文件时出现错误：{}", e))
                        .ifPresent(defFile -> DefFileLoader.add(
                                ResourceLocation.fromNamespaceAndPath(namespace, idPath),
                                defFile
                        ));
            });
        } catch (IOException e) {
            LOGGER.error("读取文件夹时出现错误：", e);
        }
    }

    @Override
    public String getName() {
        return "Sable: RagdollLib";
    }
}
