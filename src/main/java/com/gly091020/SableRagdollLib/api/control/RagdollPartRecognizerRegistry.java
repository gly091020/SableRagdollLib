package com.gly091020.SableRagdollLib.api.control;

import com.gly091020.SableRagdollLib.common.DefFileLoader;
import com.gly091020.SableRagdollLib.resource.file.RagdollDefFile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 布娃娃六部位识别注册表。
 * <p>
 * 合并顺序（后执行者优先级更高）：按注册顺序依次执行识别器，
 * 最后应用 defFile 的 extraData 显式映射。库默认注册
 * {@link DefaultRagdollPartRecognizer}，附属模组可在初始化时
 * {@link #register(IRagdollPartRecognizer)} 注册补充规则。
 * <p>
 * 识别结果按 defFile 缓存，资源包重载或服务器停止时需调用 {@link #clear()}。
 */
public final class RagdollPartRecognizerRegistry {
    private static final List<IRagdollPartRecognizer> RECOGNIZERS = new ArrayList<>();
    private static final Map<ResourceLocation, Map<PartRole, String>> CACHE = new HashMap<>();

    static {
        register(new DefaultRagdollPartRecognizer());
    }

    private RagdollPartRecognizerRegistry() {
    }

    /**
     * 注册补充识别器，注册后清空识别缓存。
     */
    public static void register(IRagdollPartRecognizer recognizer) {
        RECOGNIZERS.add(Objects.requireNonNull(recognizer));
        CACHE.clear();
    }

    /**
     * 获取指定布娃娃定义的六部位识别结果（缺识别的角色不在结果中）。
     */
    public static Map<PartRole, String> recognize(ResourceLocation defFileId) {
        return CACHE.computeIfAbsent(defFileId, RagdollPartRecognizerRegistry::load);
    }

    /**
     * 清空识别缓存。
     */
    public static void clear() {
        CACHE.clear();
    }

    private static Map<PartRole, String> load(ResourceLocation defFileId) {
        RagdollDefFile defFile = DefFileLoader.getDefFile(defFileId);
        if (defFile == null) {
            return Map.of();
        }
        Map<PartRole, String> result = new HashMap<>();
        for (IRagdollPartRecognizer recognizer : RECOGNIZERS) {
            result = recognizer.recognize(defFile, result);
        }
        // defFile 显式映射优先级最高
        result.putAll(readRolesFromExtra(defFile.extra()));
        return Map.copyOf(result);
    }

    private static Map<PartRole, String> readRolesFromExtra(CompoundTag extra) {
        Map<PartRole, String> result = new HashMap<>();
        if (!extra.contains("roles", Tag.TAG_COMPOUND)) {
            return result;
        }
        CompoundTag roles = extra.getCompound("roles");
        for (PartRole role : PartRole.values()) {
            if (roles.contains(role.name(), Tag.TAG_STRING)) {
                result.put(role, roles.getString(role.name()));
            }
        }
        return result;
    }
}