package com.gly091020.SableRagdollLib.common;

import com.gly091020.SableRagdollLib.api.RagdollTypeRegistry;
import com.gly091020.SableRagdollLib.resource.file.RagdollHitbox;
import com.gly091020.SableRagdollLib.resource.file.RagdollRenderData;
import dev.ryanhcode.sable.api.block.BlockWithSubLevelCollisionCallback;
import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import dev.ryanhcode.sable.physics.chunk.VoxelNeighborhoodState;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.physics.impl.rapier.Rapier3D;
import dev.ryanhcode.sable.physics.impl.rapier.collider.RapierVoxelColliderData;
import joptsimple.internal.Strings;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Map;

// 大力出奇迹
public class PartColliderBoxManager {
    private static final Map<String, RapierVoxelColliderData> data = new HashMap<>();

    public static void reset(){
        data.clear();
    }

    public static RapierVoxelColliderData getColliderData(ResourceLocation defFileID,
                                                          ResourceLocation type,
                                                          RagdollRenderData.PartRenderData renderData,
                                                          RagdollHitbox.PartBox blockShape){
        var k = defFileID + "/" + Strings.join(renderData.parts().stream().map(RagdollRenderData.EveryPart::partName).toList(),
                "|");
        if(data.containsKey(k))
            return data.get(k);
        var settings = RagdollTypeRegistry.getRagdollType(type);
        if(settings == null)return RapierVoxelColliderData.EMPTY;

        var r = createOne(blockShape, settings);
        data.put(k, r);
        return r;
    }

    public static RapierVoxelColliderData createOne(RagdollHitbox.PartBox shape, RagdollTypeRegistry.Settings settings){
        var childState = settings.partBlock().get().defaultBlockState();
        final boolean liquid = VoxelNeighborhoodState.isLiquid(childState);

        final double friction = PhysicsBlockPropertyHelper.getFriction(childState);
        final double volume = PhysicsBlockPropertyHelper.getVolume(childState);
        final double restitution = PhysicsBlockPropertyHelper.getRestitution(childState);
        final BlockSubLevelCollisionCallback callback = BlockWithSubLevelCollisionCallback.sable$getCallback(childState);
        final RapierVoxelColliderData entry = Rapier3D.createVoxelColliderEntry(friction, volume, restitution, liquid, callback);

        for(RagdollHitbox.Box box: shape.boxes())
            entry.addBox(new Vector3d(box.minX(), box.minY(), box.minZ()), new Vector3d(box.maxX(), box.maxY(), box.maxZ()));

        return entry;
    }
}
