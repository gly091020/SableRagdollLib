package com.gly091020.SableRagdollLib.entity;

import com.gly091020.SableRagdollLib.api.RagdollManager;
import com.gly091020.SableRagdollLib.block.AbstractPartBlockEntity;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.mixinhelpers.camera.new_camera_types.SableCameraTypes;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PartSeat extends Entity {
    private SubLevel main;

    private UUID mainUUID;
    private boolean oldSilent = false;
    private boolean oldInvisible = false;
    private boolean oldInvulnerable = false;

    private Entity onEntity;
    private boolean cameraSet = false;
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
        if (mainUUID != null)
            compoundTag.putUUID("main", mainUUID);
    }

    @Override
    public void tick() {
        Entity passenger = this.getFirstPassenger();
        if (passenger != null && passenger != onEntity) {
            onEntity = passenger;
        }

        if(level().isClientSide){
            if(cameraSet)return;
            if(Minecraft.getInstance().player != null &&
                    onEntity instanceof Player player &&
                    player.is(Minecraft.getInstance().player))
                Minecraft.getInstance().options.setCameraType(SableCameraTypes.SUB_LEVEL_VIEW_UNLOCKED);
            cameraSet = true;
            return;
        }

        if(main == null && mainUUID != null){
            var container = (ServerSubLevelContainer) ServerSubLevelContainer.getContainer(level());
            if(container != null){
                main = container.getSubLevel(mainUUID);
            }
        }
        if(onEntity != null){
            onEntity.setSilent(true);
            onEntity.setInvisible(true);
            onEntity.setInvulnerable(true);
        }
        if(main == null || main.isRemoved() || !this.isVehicle()){
            discard();
            if(main != null &&
                    main.getPlot().getEmbeddedLevelAccessor().getBlockEntity(BlockPos.ZERO) instanceof
                            AbstractPartBlockEntity partBlockEntity && partBlockEntity.getPartData().isMain()){
                var rag = RagdollManager.get(partBlockEntity.getPartData().ragdollUUID());
                if(rag != null)rag.remove();
            }
        }
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        if(!reason.shouldDestroy())return;
        ejectPassengers();
        if(onEntity != null && onEntity.isAlive()){
            onEntity.setSilent(oldSilent);
            onEntity.setInvulnerable(oldInvulnerable);
            onEntity.setInvisible(oldInvisible);
            if(onEntity instanceof Mob mob)
                mob.setNoAi(false);
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
        if(entity instanceof Mob mob)
            mob.setNoAi(true);

        entity.startRiding(this, true);
        onEntity = entity;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public @NotNull Vec3 getPassengerRidingPosition(Entity entity) {
        return position().add(0, -entity.getBbHeight() / 2, 0);
    }
}
