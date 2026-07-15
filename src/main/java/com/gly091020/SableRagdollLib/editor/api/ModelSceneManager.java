package com.gly091020.SableRagdollLib.editor.api;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class ModelSceneManager {
    private static final Map<ResourceLocation, IModelSceneSupplier> SUPPLIERS = new HashMap<>();

    public static void registry(ResourceLocation type, IModelSceneSupplier sceneSupplier){
        SUPPLIERS.put(type, sceneSupplier);
    }

    public static IModelSceneSupplier get(ResourceLocation type){
        return SUPPLIERS.get(type);
    }
}
