package com.gly091020.SableRagdollLib.common;

import com.gly091020.SableRagdollLib.resource.file.RagdollDefFile;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DefFileLoader {
    private static final Map<ResourceLocation, RagdollDefFile> DEF_FILES = new HashMap<>();

    static void clear(){
        DEF_FILES.clear();
    }

    static void add(ResourceLocation resourceLocation, RagdollDefFile defFile){
        DEF_FILES.put(resourceLocation, defFile);
    }

    public static Collection<ResourceLocation> getAllKeys(){
        return DEF_FILES.keySet();
    }

    @Nullable
    public static RagdollDefFile getDefFile(ResourceLocation id){
        return DEF_FILES.get(id);
    }
}
