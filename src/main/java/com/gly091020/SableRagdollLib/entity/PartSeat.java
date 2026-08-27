package com.gly091020.SableRagdollLib.entity;

import com.gly091020.SableRagdollLib.SableRagdollLib;
import com.gly091020.SableRagdollLib.api.RagdollManager;
import com.gly091020.SableRagdollLib.api.ScheduleManager;
import com.gly091020.SableRagdollLib.api.control.PartRole;
import com.gly091020.SableRagdollLib.block.AbstractPartBlockEntity;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PartSeat extends Entity {
    /** 主子世界 UUID，通过实体数据同步到客户端（仅服务端可见的字段客户端拿不到）。 */
    private static final EntityDataAccessor<Optional<UUID>> DATA_MAIN_UUID =
            SynchedEntityData.defineId(PartSeat.class, EntityDataSerializers.OPTIONAL_UUID);

    private SubLevel main;

    private UUID mainUUID;

    private Entity onEntity;

    /**
     * 玩家乘客的 UUID，仅在从存档读取时设置。
     * 玩家不会像普通实体一样被写进载具的 Passengers（否则加载时会生成幽灵 Player），
     * 所以单独存 UUID，等玩家上线后重新挂载。
     */
    private UUID pendingPassengerUUID;

    private static final EntityDataAccessor<Map<PartRole, UUID>> PART_ROLE =
            SynchedEntityData.defineId(PartSeat.class, SableRagdollLib.PART_ROLES.get());
    private Map<PartRole, UUID> partRoles;

    public PartSeat(EntityType<PartSeat> type, Level level) {
        super(type, level);
    }

    public void setMainSubLevel(SubLevel main) {
        this.main = main;
        this.mainUUID = main.getUniqueId();
        this.entityData.set(DATA_MAIN_UUID, Optional.of(this.mainUUID));
        var rag = RagdollManager.get(main);
        if(rag != null)
            entityData.set(PART_ROLE, rag.getPartRoles());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        builder.define(DATA_MAIN_UUID, Optional.empty());
        builder.define(PART_ROLE, Map.of());
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        if(compoundTag.contains("main")) {
            mainUUID = compoundTag.getUUID("main");
            this.entityData.set(DATA_MAIN_UUID, Optional.of(mainUUID));
        }
        if(compoundTag.hasUUID("passenger"))
            pendingPassengerUUID = compoundTag.getUUID("passenger");
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_MAIN_UUID.equals(key)) {
            this.mainUUID = this.entityData.get(DATA_MAIN_UUID).orElse(null);
        }
        if(PART_ROLE.equals(key))
            partRoles = entityData.get(PART_ROLE);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        if (mainUUID != null)
            compoundTag.putUUID("main", mainUUID);
        Entity passenger = onEntity != null ? onEntity : this.getFirstPassenger();
        if(passenger instanceof Player)
            compoundTag.putUUID("passenger", passenger.getUUID());
    }

    @Override
    public @NotNull CompoundTag saveWithoutId(@NotNull CompoundTag compoundTag) {
        CompoundTag saved = super.saveWithoutId(compoundTag);
        // 玩家乘客不能写进 Passengers，否则 chunk 加载时会创建一个幽灵 Player 实体。
        // 乘客 UUID 已在 addAdditionalSaveData 中单独保存。
        Entity passenger = onEntity != null ? onEntity : this.getFirstPassenger();
        if(passenger instanceof Player)
            saved.remove("Passengers");
        return saved;
    }

    /**
     * 原版对"恰好一个玩家乘客的载具"返回 false，导致被玩家乘坐的 PartSeat 在存档时被跳过；
     * 且玩家下线时 {@link net.minecraft.server.players.PlayerList#remove} 会因此把载具
     * setRemoved(UNLOADED_WITH_PLAYER) 并从世界移除，座位连保存的机会都没有。
     * 座位必须随存档保留，才能在重进存档时恢复乘坐，因此这里总是返回 true。
     */
    @Override
    public boolean shouldBeSaved() {
        return true;
    }

    /**
     * 玩家下线时 {@link net.minecraft.server.players.PlayerList#remove} 会检查根载具的
     * hasExactlyOnePlayerPassenger()，为 true 就把载具及乘客全部 setRemoved(UNLOADED_WITH_PLAYER)
     * （shouldSave=false），座位会从实体 section 中被移除而无法写入存档。
     * 这里始终返回 false 以阻止该清理，让座位保留在世界上并随存档保存。
     * <p>
     * 这个坑踩了 gly 整整一个晚上：原版"玩家坐过的载具不让存档"的机制有两层，第一层是 shouldBeSaved
     * 过滤，第二层是下线时直接把载具从世界上摘走。只修第一层，存档里就只有一坨被清空的空气。
     * 现在两层都堵上了，重进存档还能坐在布娃娃上，奖励 deepseek 一个🍡。
     */
    @Override
    public boolean hasExactlyOnePlayerPassenger() {
        return false;
    }

    @Override
    public void tick() {
        Entity passenger = this.getFirstPassenger();
        if (passenger != null && passenger != onEntity) {
            onEntity = passenger;
        }
        // 乘客已就位（例如被其它机制恢复），不再等待恢复
        if (passenger != null && passenger.getUUID().equals(pendingPassengerUUID)) {
            pendingPassengerUUID = null;
        }

        if(level().isClientSide){
            return;
        }

        if(main == null && mainUUID != null){
            var container = (ServerSubLevelContainer) ServerSubLevelContainer.getContainer(level());
            if(container != null){
                main = container.getSubLevel(mainUUID);
            }
        }
        if(tickCount <= 20)return;

        boolean waitingPassenger = pendingPassengerUUID != null && !this.isVehicle();
        if(waitingPassenger && main != null && !main.isRemoved()){
            var entity = ((ServerLevel) level()).getEntity(pendingPassengerUUID);
            if(entity instanceof Player player && player.isAlive() && player.getVehicle() == null){
                rideMe(player);
                pendingPassengerUUID = null;
                waitingPassenger = false;
            }
        }

        if(!waitingPassenger && (main == null || main.isRemoved() || !this.isVehicle())){
            if(main != null &&
                    main.getPlot().getEmbeddedLevelAccessor().getBlockEntity(BlockPos.ZERO) instanceof
                            AbstractPartBlockEntity partBlockEntity && partBlockEntity.getPartData().isMain()){
                var rag = RagdollManager.get(partBlockEntity.getPartData().ragdollUUID());
                if(rag != null)rag.remove();
            }
            ScheduleManager.scheduleDelayed((ServerLevel) level(), 2, this::discard);
        }
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        if(!reason.shouldDestroy())return;
        ejectPassengers();
        if(onEntity != null && onEntity.isAlive()){
            onEntity.setDeltaMovement(Vec3.ZERO);
            if(onEntity instanceof LivingEntity livingEntity)
                livingEntity.addEffect(new MobEffectInstance(
                        MobEffects.DAMAGE_RESISTANCE, 10, 255, false, false, false
                ));
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
        entity.startRiding(this, true);
        onEntity = entity;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public @NotNull Vec3 getPassengerRidingPosition(Entity entity) {
        if(entity instanceof Player)
            return position().add(0, -entity.getBbHeight() / 4, 0);
        return position().add(0, -entity.getBbHeight(), 0);
    }

    public UUID getMainUUID() {
        var uuid = this.entityData.get(DATA_MAIN_UUID).orElse(null);
        return uuid != null ? uuid : mainUUID;
    }

    public Map<PartRole, UUID> getPartRoles() {
        return partRoles == null ? Collections.emptyMap() : partRoles;
    }
}
