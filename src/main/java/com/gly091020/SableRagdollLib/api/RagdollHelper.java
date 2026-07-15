package com.gly091020.SableRagdollLib.api;

import com.gly091020.SableRagdollLib.block.AbstractPartBlockEntity;
import com.gly091020.SableRagdollLib.common.DefFileLoader;
import com.gly091020.SableRagdollLib.resource.file.RagdollJoints;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3dc;

import java.util.*;
import java.util.function.Consumer;

public class RagdollHelper {
    public static Ragdoll createRagdoll(ServerLevel serverLevel, Vec3 pos, ResourceLocation id){
        var defFile = DefFileLoader.getDefFile(id);
        if(defFile == null)return null;
        var settings = RagdollTypeRegistry.getRagdollType(defFile.type());
        ServerSubLevelContainer container = SubLevelContainer.getContainer(serverLevel);
        if(settings == null || container == null)return null;

        var allPart = new HashMap<String, ServerSubLevel>();
        var allBE = new HashMap<String, AbstractPartBlockEntity>();
        var ragdollUUID = UUID.randomUUID();
        for (String part: defFile.allParts()){
            var data = defFile.position().position().get(part);
            if(data == null)continue;
            var s = createBlock(container, settings, blockEntity ->
            {
                var isMain = defFile.mainBody().isPresent() && defFile.mainBody().get().equals(part);
                blockEntity.setData(new AbstractPartBlockEntity.Data(
                        isMain,
                        part,
                        ragdollUUID,
                        id, defFile.type(),
                        defFile.hitbox().hitbox().get(part),
                        defFile.renderData().renderData().get(part),
                        Optional.empty(),
                        defFile.expressions(),
                        List.of()
                ));
                allBE.put(part, blockEntity);
            }, data.getPose(pos));
            if(s != null)
                allPart.put(part, s);
        }
        if(allBE.isEmpty())return null;
        AbstractPartBlockEntity body = null;
        if(defFile.mainBody().isPresent())
            body = allBE.get(defFile.mainBody().get());
        if(body == null)
            body = allBE.values().stream().toList().getFirst();

        var data = new ArrayList<AbstractPartBlockEntity.JointDataWithSublevel>();
        for (RagdollJoints.JointData jointData: defFile.joints().jointData()){
            var a = allPart.get(jointData.a());
            var b = allPart.get(jointData.b());
            if(a == null || b == null)continue;

            var pos1 = JOMLConversion.toMojang(jointData.getVector3dcA(a));
            var pos2 = JOMLConversion.toMojang(jointData.getVector3dcB(b));
            data.add(new AbstractPartBlockEntity.JointDataWithSublevel(
                    jointData, a.getUniqueId(), b.getUniqueId(), pos1, pos2
            ));
        }
        body.setData(new AbstractPartBlockEntity.Data(
                true,
                body.getPartData().partName(),
                body.getPartData().ragdollUUID(),
                body.getPartData().defFile(),
                body.getPartData().type(),
                body.getPartData().hitbox(),
                body.getPartData().renderData(),
                Optional.of(data),
                body.getPartData().expressions(),
                allPart.values().stream().map(ServerSubLevel::getUniqueId).toList()
        ));
        var r = new Ragdoll(allPart.values().stream().toList());
        RagdollManager.add(r);
        return r;
    }

    public static ServerSubLevel createBlock(ServerSubLevelContainer container,
                                             RagdollTypeRegistry.Settings settings,
                                             Consumer<AbstractPartBlockEntity> dataConsumer,
                                             Pose3d pose3d) {
        final BlockState blockState = settings.partBlock().get().defaultBlockState();
        if (container == null) return null;
        var subLevel = container.allocateNewSubLevel(pose3d);
        var plot = subLevel.getPlot();
        var center = plot.getCenterChunk();
        plot.newEmptyChunk(center);
        plot.getEmbeddedLevelAccessor().setBlock(BlockPos.ZERO, blockState, 3);
        var be = plot.getEmbeddedLevelAccessor().getBlockEntity(BlockPos.ZERO);
        if (be instanceof AbstractPartBlockEntity blockEntity)
            dataConsumer.accept(blockEntity);

        plot.getEmbeddedLevelAccessor().setBlock(BlockPos.ZERO.above(), Blocks.GLASS.defaultBlockState(), 3);
        plot.getEmbeddedLevelAccessor().setBlock(BlockPos.ZERO.above(), Blocks.AIR.defaultBlockState(), 3);

        subLevel.updateLastPose();
        return (ServerSubLevel) subLevel;
    }

    public static PhysicsConstraintHandle createJoint(ServerSubLevelContainer container,
                                                      ServerSubLevel subLevel1,
                                                      ServerSubLevel subLevel2,
                                                      Vector3dc pos1,
                                                      Vector3dc pos2){
        return container.physicsSystem().getPipeline().addConstraint(subLevel1, subLevel2,
                new GenericConstraintConfiguration(pos1, pos2, new Quaterniond(), new Quaterniond(),
                        Set.of(ConstraintJointAxis.LINEAR_X, ConstraintJointAxis.LINEAR_Y, ConstraintJointAxis.LINEAR_Z)));
    }

    public static PhysicsConstraintHandle createJoint(ServerSubLevelContainer container,
                                                      ServerSubLevel subLevel1,
                                                      ServerSubLevel subLevel2,
                                                      Vec3 pos1,
                                                      Vec3 pos2){
        return createJoint(container, subLevel1, subLevel2, JOMLConversion.toJOML(pos1), JOMLConversion.toJOML(pos2));
    }
}
