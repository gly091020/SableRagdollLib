package com.gly091020.SableRagdollLib.api;

import com.gly091020.SableRagdollLib.block.AbstractPartBlock;
import com.gly091020.SableRagdollLib.block.AbstractPartBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class RagdollTypeRegistry {
    private static final Map<ResourceLocation, Settings> SETTINGS = new HashMap<>();

    public static void registry(ResourceLocation id, Supplier<AbstractPartBlock> partBlock, Supplier<BlockEntityType<? extends AbstractPartBlockEntity>> partBE){
        SETTINGS.put(id, new Settings(partBlock, partBE));
    }

    public static Settings getRagdollType(ResourceLocation id){
        return SETTINGS.get(id);
    }

    public record Settings(Supplier<AbstractPartBlock> partBlock,
                           Supplier<BlockEntityType<? extends AbstractPartBlockEntity>> partBE){ }

    public record Abilities(boolean fracture, boolean interact){}

    public static Abilities getRagdollTypeAbilities(ResourceLocation id){
        return new Abilities(false, true);
    }

    public static Collection<ResourceLocation> getAllType(){
        return SETTINGS.keySet();
    }
}
