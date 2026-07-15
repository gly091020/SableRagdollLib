package com.gly091020.SableRagdollLib.api;

import com.gly091020.SableRagdollLib.SableRagdollLib;
import com.gly091020.SableRagdollLib.block.AbstractPartBlockEntity;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

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

        if(SableRagdollLib.config.enableForceLoad){
            var container = ServerSubLevelContainer.getContainer(level);
            if(container == null)return;
            subLevels.forEach(subLevel -> container.addForceLoadTicket(subLevel, SubLevelLoadingTicketType.COMMAND_FORCED, null));
        }
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
            if(!partBlockEntity.getPartData().isMain())return;
            partBlockEntity.addEntity(entity);
            break;
        }
    }

    public void addLinearImpulse(Vec3 value){
        var container = ServerSubLevelContainer.getContainer(level);
        if(container == null)return;
        getSublevels().forEach(subLevel ->
                container.physicsSystem().getPhysicsHandle(subLevel).applyLinearImpulse(JOMLConversion.toJOML(value)));
    }

    public void addAngularImpulse(Vec3 value){
        var container = ServerSubLevelContainer.getContainer(level);
        if(container == null)return;

        var mainSub = container.getSubLevel(main);
        if(mainSub == null)return;

        container.physicsSystem()
                .getPhysicsHandle((ServerSubLevel) mainSub)
                .applyAngularImpulse(JOMLConversion.toJOML(value));
    }
}
