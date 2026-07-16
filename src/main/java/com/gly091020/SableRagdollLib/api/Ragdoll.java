package com.gly091020.SableRagdollLib.api;

import com.gly091020.SableRagdollLib.SableRagdollLib;
import com.gly091020.SableRagdollLib.block.AbstractPartBlockEntity;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Ragdoll {
    private final UUID uuid;
    private final ServerLevel level;
    private final List<UUID> subLevelUUIDList = new ArrayList<>();
    private final UUID main;
    private boolean alive = true;
    private final List<PhysicsConstraintHandle> joints = new ArrayList<>();

    public Ragdoll(ServerSubLevel... subLevels){
        this(Arrays.stream(subLevels).toList());
    }

    public Ragdoll(List<ServerSubLevel> subLevels){
        if(subLevels.isEmpty())throw new RuntimeException("没有子维度");
        for(SubLevel subLevel: subLevels)
            subLevelUUIDList.add(subLevel.getUniqueId());
        UUID center = null;
        AbstractPartBlockEntity centerBE = null;
        for (SubLevel subLevel: subLevels){
            if(subLevel.getPlot().getEmbeddedLevelAccessor().getBlockEntity(BlockPos.ZERO) instanceof AbstractPartBlockEntity partBlockEntity && partBlockEntity.getPartData().isMain()) {
                center = subLevel.getUniqueId();
                centerBE = partBlockEntity;
                break;
            }
        }
        if(center == null)throw new RuntimeException("没有中心部位");
        this.main = center;
        this.uuid = centerBE.getPartData().ragdollUUID();
        this.level = subLevels.getFirst().getLevel();

        var container = ServerSubLevelContainer.getContainer(level);
        if(container == null)return;
        if(SableRagdollLib.config.enableForceLoad){
            subLevels.forEach(subLevel -> container.addForceLoadTicket(subLevel, SubLevelLoadingTicketType.COMMAND_FORCED, null));
        }

        container.addObserver(new SubLevelObserver() {
            @Override
            public void onSubLevelRemoved(SubLevel subLevel, SubLevelRemovalReason reason) {
                if(reason != SubLevelRemovalReason.REMOVED)return;
                if(subLevel instanceof ServerSubLevel serverSubLevel && subLevelUUIDList.contains(serverSubLevel.getUniqueId()))
                    remove();
            }
        });
    }

    public UUID getUuid() {
        return uuid;
    }

    public UUID getCenter() {
        return main;
    }

    public boolean isAlive() {
        return alive;
    }

    public boolean isLoad(){
        if(!alive)return false;
        var container = ServerSubLevelContainer.getContainer(level);
        if(container == null)return false;
        for (UUID sub: subLevelUUIDList)
            if(container.getSubLevel(sub) == null)return false;
        return true;
    }

    public void remove(){
        if(!alive)return;
        alive = false;
        var container = ServerSubLevelContainer.getContainer(level);
        if(container == null)return;
        for (UUID sub: subLevelUUIDList){
            var subLevel = container.getSubLevel(sub);
            if(subLevel == null || subLevel.isRemoved())continue;
            subLevel.markRemoved();
        }
    }

    public void connectJoint(List<PhysicsConstraintHandle> joints){
        this.joints.forEach(physicsConstraintHandle -> {
            if(physicsConstraintHandle.isValid())physicsConstraintHandle.remove();
        });
        this.joints.clear();
        this.joints.addAll(joints);
    }

    public List<ServerSubLevel> getSublevels(){
        var container = ServerSubLevelContainer.getContainer(level);
        if(container == null)return List.of();
        var r = new ArrayList<ServerSubLevel>();
        subLevelUUIDList.forEach(uuid1 -> {
            var s = container.getSubLevel(uuid1);
            if(s != null)r.add((ServerSubLevel) s);
        });
        return r;
    }

    public void addEntity(Entity entity){
        var subs = getSublevels();
        for (SubLevel subLevel: subs){
            if(!(subLevel.getPlot().getEmbeddedLevelAccessor().getBlockEntity(BlockPos.ZERO) instanceof AbstractPartBlockEntity partBlockEntity))return;
            if(!partBlockEntity.getPartData().isMain())continue;
            partBlockEntity.addEntity(entity);
            break;
        }
    }

    public void addLinearImpulse(Vector3d value, boolean local){
        var container = ServerSubLevelContainer.getContainer(level);
        if(container == null)return;
        getSublevels().forEach(subLevel -> {
            var f = new Vector3d(value);
            if(local)subLevel.logicalPose().transformNormalInverse(f);
            container.physicsSystem().getPhysicsHandle(subLevel).applyLinearImpulse(f);
        });
    }

    public void addLinearImpulse(Vec3 value, boolean local){
        addLinearImpulse(JOMLConversion.toJOML(value), local);
    }

    public void addAngularImpulse(Vector3d value, boolean local){
        var container = ServerSubLevelContainer.getContainer(level);
        if(container == null)return;

        var mainSub = container.getSubLevel(main);
        if(mainSub == null)return;

        var f = new Vector3d(value);
        if(local)mainSub.logicalPose().transformNormalInverse(f);
        container.physicsSystem()
                .getPhysicsHandle((ServerSubLevel) mainSub)
                .applyAngularImpulse(f);
    }

    public void addAngularImpulse(Vec3 value, boolean local){
        addAngularImpulse(JOMLConversion.toJOML(value), local);
    }
}
