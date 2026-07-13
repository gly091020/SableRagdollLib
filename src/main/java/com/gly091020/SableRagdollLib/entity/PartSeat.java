package com.gly091020.SableRagdollLib.entity;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PartSeat extends Entity {
    private SubLevel main;

    private UUID mainUUID;
    private boolean oldSilent = false;
    private boolean oldInvisible = false;
    private boolean oldInvulnerable = false;

    private Entity onEntity;
    public PartSeat(EntityType<PartSeat> type, Level level) {
        super(type, level);
    }

    public void setMainSubLevel(SubLevel main) {
        this.main = main;
        mainUUID = main.getUniqueId();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {

    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        if(compoundTag.contains("silent", Tag.TAG_BYTE))
            oldSilent = compoundTag.getBoolean("silent");
        if(compoundTag.contains("invisible", Tag.TAG_BYTE))
            oldInvisible = compoundTag.getBoolean("invisible");
        if(compoundTag.contains("invulnerable", Tag.TAG_BYTE))
            oldInvulnerable = compoundTag.getBoolean("invulnerable");
        if(compoundTag.contains("main"))
            mainUUID = compoundTag.getUUID("main");
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        compoundTag.putBoolean("silent", oldSilent);
        compoundTag.putBoolean("invisible", oldInvisible);
        compoundTag.putBoolean("invulnerable", oldInvulnerable);
        if (mainUUID != null) {
            compoundTag.putUUID("main", mainUUID);
        }
    }

    @Override
    public void tick() {
        if(level().isClientSide)return;
        if(main == null && mainUUID != null){
            var container = (ServerSubLevelContainer) ServerSubLevelContainer.getContainer(level());
            if(container != null){
                main = container.getSubLevel(mainUUID);
            }
        }
        Entity passenger = this.getFirstPassenger();
        if (passenger != null && passenger != onEntity) {
            onEntity = passenger;
        }
        if(onEntity != null){
            onEntity.setSilent(true);
            onEntity.setInvisible(true);
            onEntity.setInvulnerable(true);
        }
        if(main == null || main.isRemoved() || !this.isVehicle()){
            discard();
        }
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        ejectPassengers();
        if(onEntity != null && onEntity.isAlive()){
            onEntity.setSilent(oldSilent);
            onEntity.setInvulnerable(oldInvulnerable);
            onEntity.setInvisible(oldInvisible);
        }
        onEntity = null;
        super.remove(reason);
    }

    @Override
    public boolean isInvisible() {
        return true;
    }

    @Override
    protected boolean canAddPassenger(@NotNull Entity passenger) {
        return this.getPassengers().isEmpty();
    }

    public void rideMe(Entity entity){
        oldInvisible = entity.isInvisible();
        oldSilent = entity.isSilent();
        oldInvulnerable = entity.isInvulnerable();

        entity.setSilent(true);
        entity.setInvisible(true);
        entity.setInvulnerable(true);

        entity.startRiding(this, true);
        onEntity = entity;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
