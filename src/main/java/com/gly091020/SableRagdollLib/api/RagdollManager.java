package com.gly091020.SableRagdollLib.api;

import com.gly091020.SableRagdollLib.block.AbstractPartBlockEntity;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RagdollManager {
    private static final Map<UUID, Ragdoll> RAGDOLLS = new HashMap<>();

    public static void add(Ragdoll ragdoll){
        RAGDOLLS.put(ragdoll.getUuid(), ragdoll);
    }

    @Nullable
    public static Ragdoll get(UUID uuid){
        return RAGDOLLS.get(uuid);
    }

    public static void tick(){
        ArrayList<UUID> removes = new ArrayList<>();
        RAGDOLLS.forEach((k, v) -> {
            if(!v.isAlive())removes.add(k);
        });
        removes.forEach(RAGDOLLS::remove);
    }

    /** 当前已注册的全部布娃娃（含已失效的，调用方自行过滤）。 */
    public static ArrayList<Ragdoll> getAll(){
        return new ArrayList<>(RAGDOLLS.values());
    }

    public static void reset(){
        RAGDOLLS.clear();
    }

    @Nullable
    public static Ragdoll get(AbstractPartBlockEntity blockEntity){
        return get(blockEntity.getPartData().ragdollUUID());
    }

    @Nullable
    public static Ragdoll get(SubLevel subLevel){
        if(!(subLevel.getPlot().getEmbeddedLevelAccessor().getBlockEntity(BlockPos.ZERO) instanceof AbstractPartBlockEntity blockEntity))
            return null;
        return get(blockEntity);
    }

    @Nullable
    public static Ragdoll get(Level level, BlockPos blockPos){
        var subLevel = (SubLevel) SableCompanion.INSTANCE.getContaining(level, blockPos);
        if(subLevel == null)return null;
        return get(subLevel);
    }

    @Nullable
    public static Ragdoll get(Level level, UUID subLevelUUID){
        var c = SubLevelContainer.getContainer(level);
        if(c == null)return null;
        var subLevel = c.getSubLevel(subLevelUUID);
        if(subLevel == null)return null;
        return get(subLevel);
    }
}
