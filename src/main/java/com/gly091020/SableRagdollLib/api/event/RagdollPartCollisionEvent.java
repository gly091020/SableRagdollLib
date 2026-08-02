package com.gly091020.SableRagdollLib.api.event;

import com.gly091020.SableRagdollLib.block.AbstractPartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public abstract class RagdollPartCollisionEvent extends Event {
    private final BlockPos pos1;
    @Nullable
    private final BlockPos pos2;
    private final AbstractPartBlockEntity selfBE;
    private final double impactVelocity;

    public RagdollPartCollisionEvent(BlockPos pos1, @Nullable BlockPos pos2, AbstractPartBlockEntity selfBE, double impactVelocity){
        this.selfBE = selfBE;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.impactVelocity = impactVelocity;
    }

    public AbstractPartBlockEntity getSelfBE() {
        return selfBE;
    }

    public BlockPos getPos1() {
        return pos1;
    }

    public @Nullable BlockPos getPos2() {
        return pos2;
    }

    public double getImpactVelocity() {
        return impactVelocity;
    }

    public Level getLevel(){
        return Objects.requireNonNull(selfBE.getLevel());
    }

    public static class Pre extends RagdollPartCollisionEvent implements ICancellableEvent{
        public Pre(BlockPos pos1, @Nullable BlockPos pos2, AbstractPartBlockEntity selfBE, double impactVelocity) {
            super(pos1, pos2, selfBE, impactVelocity);
        }
    }

    public static class Post extends RagdollPartCollisionEvent implements ICancellableEvent{
        public Post(BlockPos pos1, @Nullable BlockPos pos2, AbstractPartBlockEntity selfBE, double impactVelocity) {
            super(pos1, pos2, selfBE, impactVelocity);
        }
    }
}
