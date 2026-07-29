package com.gly091020.SableRagdollLib.block;

import com.gly091020.SableRagdollLib.SableRagdollLib;
import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public class AbstractPartBlockCallBack implements BlockSubLevelCollisionCallback {
    public static final AbstractPartBlockCallBack INSTANCE = new AbstractPartBlockCallBack();

    @Override
    public CollisionResult sable$onCollision(BlockPos hitBlockPos, @Nullable BlockPos otherHitBlockPos, Vector3d impactPosition, double impactVelocity) {
        if(!SableRagdollLib.config.enableHurt)return CollisionResult.NONE;
        var level = SubLevelPhysicsSystem.getCurrentlySteppingSystem().getLevel();
        if(level.getBlockEntity(hitBlockPos) instanceof AbstractPartBlockEntity self && self.getEntity() != null){
            if(otherHitBlockPos != null &&
                    level.getBlockEntity(otherHitBlockPos) instanceof AbstractPartBlockEntity target &&
                    target.getEntity() != null &&
                    target.getEntity().is(self.getEntity()))return CollisionResult.NONE;
            hurt(level, self.getEntity(), impactVelocity);
        }
        return CollisionResult.NONE;
    }

    private void hurt(Level level, Entity entity, double impactVelocity){
        if(entity == null || !entity.isAlive())return;
        if(impactVelocity * impactVelocity < 64)return;
        entity.invulnerableTime = 0;
        entity.hurt(level.damageSources().flyIntoWall(), (float) (impactVelocity / 60));
        if(!entity.isAlive()){
            entity.stopRiding();
        }
    }
}
