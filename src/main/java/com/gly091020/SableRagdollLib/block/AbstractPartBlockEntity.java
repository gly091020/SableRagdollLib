package com.gly091020.SableRagdollLib.block;

import com.gly091020.SableRagdollLib.SableRagdollLib;
import com.gly091020.SableRagdollLib.api.Ragdoll;
import com.gly091020.SableRagdollLib.api.RagdollHelper;
import com.gly091020.SableRagdollLib.api.RagdollManager;
import com.gly091020.SableRagdollLib.api.RagdollTypeRegistry;
import com.gly091020.SableRagdollLib.entity.PartSeat;
import com.gly091020.SableRagdollLib.resource.file.RagdollHitbox;
import com.gly091020.SableRagdollLib.resource.file.RagdollJoints;
import com.gly091020.SableRagdollLib.resource.file.RagdollRenderData;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public abstract class AbstractPartBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private Data data;
    private VoxelShape shape = Shapes.block();
    public AbstractPartBlockEntity(BlockEntityType<? extends AbstractPartBlockEntity> entityType,
                                   BlockPos pos, BlockState state) {
        super(entityType, pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if(data != null)
            Data.CODEC.encodeStart(NbtOps.INSTANCE, data)
                    .resultOrPartial(e -> LOGGER.error("序列化错误:{}", e))
                    .ifPresent(r -> tag.put("data", r));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if(tag.contains("data", Tag.TAG_COMPOUND))
            Data.CODEC.parse(NbtOps.INSTANCE, tag.get("data"))
                    .resultOrPartial(e -> LOGGER.error("加载时错误:{}", e))
                    .ifPresent(this::setData);
    }

    public void setData(Data data){
        this.data = data;
        if(data == null)return;
        shape = data.hitbox().toVoxelShape();
    }

    public Data getPartData() {
        return data;
    }

    public VoxelShape getShape(){
        return shape;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        updateBlockShape();
    }

    public void updateBlockShape(){
        if(level == null)return;
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 8);
    }

    public record Data(
            boolean isMain,
            String partName,
            UUID ragdollUUID,
            ResourceLocation defFile,
            ResourceLocation type,
            RagdollHitbox.PartBox hitbox,
            RagdollRenderData.PartRenderData renderData,
            Optional<List<JointDataWithSublevel>> jointData,
            List<UUID> subLevels){
        public static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.BOOL.fieldOf("isMain").forGetter(Data::isMain),
                Codec.STRING.fieldOf("partName").forGetter(Data::partName),
                UUIDUtil.CODEC.fieldOf("ragdollUUID").forGetter(Data::ragdollUUID),
                ResourceLocation.CODEC.fieldOf("defFile").forGetter(Data::defFile),
                ResourceLocation.CODEC.fieldOf("type").forGetter(Data::type),
                RagdollHitbox.PartBox.CODEC.fieldOf("hitbox").forGetter(Data::hitbox),
                RagdollRenderData.PartRenderData.CODEC.fieldOf("renderData").forGetter(Data::renderData),
                JointDataWithSublevel.CODEC.listOf().optionalFieldOf("jointData").forGetter(Data::jointData),
                UUIDUtil.CODEC.listOf().fieldOf("subLevels").forGetter(Data::subLevels)
        ).apply(i, Data::new));
    }

    public record JointDataWithSublevel(RagdollJoints.JointData origin, UUID a, UUID b, Vec3 posA, Vec3 posB){
        public static final Codec<JointDataWithSublevel> CODEC = RecordCodecBuilder.create(i -> i.group(
                RagdollJoints.JointData.CODEC.fieldOf("origin").forGetter(JointDataWithSublevel::origin),
                UUIDUtil.CODEC.fieldOf("a").forGetter(JointDataWithSublevel::a),
                UUIDUtil.CODEC.fieldOf("b").forGetter(JointDataWithSublevel::b),
                Vec3.CODEC.fieldOf("posA").forGetter(JointDataWithSublevel::posA),
                Vec3.CODEC.fieldOf("posB").forGetter(JointDataWithSublevel::posB)
        ).apply(i, JointDataWithSublevel::new));
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider provider) {
        return saveWithoutMetadata(provider);
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        loadAdditional(tag, provider);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void removeSelf(){
        if(level == null || level.isClientSide)return;
        var r = RagdollManager.get(data.ragdollUUID);
        if(r == null)return;
        r.remove();
    }

    public void createJoint(){
        if(level == null || data == null || level.isClientSide || data.jointData.isEmpty())return;
        var container = (ServerSubLevelContainer) ServerSubLevelContainer.getContainer(level);
        if(container == null)return;
        var self = (ServerSubLevel) SableCompanion.INSTANCE.getContaining(level, getBlockPos());
        if(self == null)return;
        var joints = new ArrayList<PhysicsConstraintHandle>();
        for (JointDataWithSublevel beJointData: data.jointData.get()){
            var a = (ServerSubLevel) container.getSubLevel(beJointData.a);
            var b = (ServerSubLevel) container.getSubLevel(beJointData.b);
            if(a == null || b == null)return;

            PhysicsConstraintHandle handle;
            try{
                handle = RagdollHelper.createJoint(container, a, b, beJointData.posA, beJointData.posB);
            } catch (Exception e) {
                LOGGER.debug("连接时错误：", e);
                continue;
            }
            joints.add(handle);
            var settings = beJointData.origin.jointSettings();
            if(settings.isEmpty())continue;
            handle.setContactsEnabled(settings.get().contacts());
            var motor = settings.get().jointMotor();
            if(motor.isEmpty())continue;
            var motorData = motor.get();
            var target = computeAxisTargets(a.logicalPose().orientation(), b.logicalPose().orientation());
            handle.setMotor(ConstraintJointAxis.ANGULAR_X, target.x, motorData.stiffness(), motorData.damping(), false, 0.0f);
            handle.setMotor(ConstraintJointAxis.ANGULAR_Y, target.y, motorData.stiffness(), motorData.damping(), false, 0.0f);
            handle.setMotor(ConstraintJointAxis.ANGULAR_Z, target.z, motorData.stiffness(), motorData.damping(), false, 0.0f);
        }

        var r = RagdollManager.get(data.ragdollUUID);
        if(r == null)return;
        r.connectJoint(joints);
    }

    public void addRagdoll(){
        if(level == null || level.isClientSide || data == null || !data.isMain)return;
        var container = (ServerSubLevelContainer) ServerSubLevelContainer.getContainer(level);
        if(container == null)return;
        var alive = true;
        var subLevels = new ArrayList<ServerSubLevel>();
        for (UUID uuid: data.subLevels){
            var subLevel = container.getSubLevel(uuid);
            if(subLevel == null){alive = false;continue;}
            subLevels.add((ServerSubLevel) subLevel);
        }
        if(!alive && !RagdollTypeRegistry.getRagdollTypeAbilities(data.type).fracture()){
            subLevels.forEach(subLevel -> {
                if(!subLevel.isRemoved())subLevel.markRemoved();
            });
            return;
        }
        RagdollManager.add(new Ragdoll(subLevels));
    }

    private static Vec3 computeAxisTargets(Quaterniond from, Quaterniond to) {
        Quaterniond delta = new Quaterniond(from).conjugate().mul(to);
        Vector3d euler = delta.getEulerAnglesXYZ(new Vector3d());
        return new Vec3(euler.x, euler.y, euler.z);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        createJoint();
        addRagdoll();
    }

    public void addEntity(Entity entity){
        if(level == null)return;
        var subLevel = (ServerSubLevel) SableCompanion.INSTANCE.getContaining(level, getBlockPos());
        if(subLevel == null)return;

        var seat = new PartSeat(SableRagdollLib.PART_SEAT.get(), level);
        seat.setPos(JOMLConversion.toMojang(RagdollJoints.JointData.localToWorld(subLevel.logicalPose(), new Vector3d())));
        seat.setMainSubLevel(subLevel);

        entity.level().addFreshEntity(seat);
        seat.rideMe(entity);
    }
}
