package com.gly091020.SableRagdollLib.api;

import com.gly091020.SableRagdollLib.block.AbstractPartBlockEntity;
import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 服务端布娃娃拖拽管理。
 * <p>
 * 拖拽使用"自由 + 线性轴电机"的软约束（关节类型仍是
 * {@link GenericConstraintConfiguration}，与 {@code RagdollHelper.createJoint}
 * 相同，但轴不锁定，靠电机驱动）：
 * <ul>
 *     <li>世界侧约束帧固定在抓取瞬间的世界坐标位置，部件侧帧固定在抓取点，之后不再移动。</li>
 *     <li>每个 tick 把三个线性轴电机的目标设为"目标点相对世界侧帧的位移"，
 *     电机像弹簧一样把抓取点拉向目标点——部件会滞后、悬挂、随摆动自然运动，
 *     也就是"用绳子拖着"的手感，而不是刚性钉在准星上。</li>
 *     <li>电机有最大力限制，因此当部件碰到地面时接触力会顶住电机，部件会贴着地面被拖，
 *     不会被硬拽进地里（无需额外的高度图钳制）。</li>
 *     <li>角轴保持自由，部件绕抓取点自然旋转摆动。</li>
 * </ul>
 * <p>
 * 坐标系约定（与 {@code RagdollHelper.createJoint} / {@code RagdollJoints} 一致）：
 * <ul>
 *     <li>部件侧锚点 {@code anchor}：位于子维度嵌入式维度的方块坐标，也就是
 *     {@code RagdollJoints.JointData.getVector3dcA/B} 计算出的关节锚点所在空间
 *     （客户端射线命中点与之同一空间）。</li>
 *     <li>世界侧帧：主世界坐标（必须不在 plotgrid 内），由
 *     {@code pose.transformPosition} 投影出的抓取点决定；目标点同样使用主世界坐标。</li>
 * </ul>
 */
public class RagdollDragManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** 开始拖拽时抓取点离玩家眼睛的最大距离（方块） */
    private static final double PICK_RANGE = 5;
    /** 拖拽过程中抓取点离玩家眼睛的最大距离，超出视为脱手（绳长上限） */
    private static final double MAX_RANGE = 5;
    /** 目标点离玩家眼睛的最大距离，防止把目标发送到任意远处 */
    private static final double MAX_TARGET_DISTANCE = 5;
    /** 线性电机刚度（×质量，N/m 量级）：越小绳子感越明显、越容易滞后，越大越跟手 */
    private static final double STIFFNESS = 300;
    /** 线性电机阻尼（×质量）：防止来回振荡 */
    private static final double DAMPING = 20;
    /** 线性电机最大力（×质量）：保证拉得动布娃娃，但不会压过地面接触 */
    private static final double MAX_FORCE = 300;

    private static final Map<UUID, DragSession> DRAGS = new HashMap<>();

    private static final class DragSession {
        final ServerPlayer player;
        final ServerSubLevel subLevel;
        final Vec3 anchor;
        final UUID ragdollUUID;
        final GenericConstraintHandle joint;
        /** 世界侧约束帧的位置（固定不移动，主世界坐标） */
        final Vector3d frame1Pos;
        Vec3 target;

        DragSession(ServerPlayer player, ServerSubLevel subLevel, Vec3 anchor, UUID ragdollUUID,
                    GenericConstraintHandle joint, Vector3d frame1Pos, Vec3 target) {
            this.player = player;
            this.subLevel = subLevel;
            this.anchor = anchor;
            this.ragdollUUID = ragdollUUID;
            this.joint = joint;
            this.frame1Pos = frame1Pos;
            this.target = target;
        }
    }

    public static void startDrag(ServerLevel level, Player player, UUID subLevelUUID, Vec3 anchor, Vec3 target) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        var container = (ServerSubLevelContainer) ServerSubLevelContainer.getContainer(level);
        if (container == null) return;
        var subLevel = container.getSubLevel(subLevelUUID);
        if (!(subLevel instanceof ServerSubLevel serverSubLevel) || serverSubLevel.isRemoved()) return;

        // 校验目标子维度确实是布娃娃部件
        if (!(serverSubLevel.getPlot().getEmbeddedLevelAccessor().getBlockEntity(BlockPos.ZERO)
                instanceof AbstractPartBlockEntity partBlockEntity)) return;
        var partData = partBlockEntity.getPartData();
        if (partData == null) return;
        var ragdoll = RagdollManager.get(partData.ragdollUUID());
        if (ragdoll == null || !ragdoll.isAlive()) return;

        if(ragdoll.getEntity() != null && ragdoll.getEntity().is(player))return;

        // 拾取距离校验（锚点在子维度局部坐标，用 Sable 官方距离辅助函数换算到世界坐标）
        if (!withinRange(level, serverPlayer, anchor, PICK_RANGE)) return;

        // 抓取点在世界坐标下的位置（与 Sable projectOutOfSubLevel / 距离计算同一约定）
        var pose = serverSubLevel.logicalPose();
        var worldAnchor = pose.transformPosition(JOMLConversion.toJOML(anchor), new Vector3d());

        // 创建"世界 <-> 部件"自由约束：轴不锁定，线性轴由电机驱动
        GenericConstraintHandle joint;
        try {
            joint = container.physicsSystem().getPipeline().addConstraint(
                    null, serverSubLevel, new GenericConstraintConfiguration(
                            worldAnchor,
                            JOMLConversion.toJOML(anchor),
                            new Quaterniond(),
                            new Quaterniond(),
                            Set.of()
                    ));
        } catch (Exception e) {
            LOGGER.debug("创建拖拽约束失败：", e);
            return;
        }
        if (joint == null || !joint.isValid()) return;

        DRAGS.put(serverPlayer.getUUID(), new DragSession(
                serverPlayer, serverSubLevel, anchor, partData.ragdollUUID(), joint, worldAnchor, target
        ));
    }

    public static void updateDrag(ServerLevel level, Player player, Vec3 target) {
        var session = DRAGS.get(player.getUUID());
        if (session == null) return;
        if (player.getEyePosition().distanceToSqr(target) > MAX_TARGET_DISTANCE * MAX_TARGET_DISTANCE) {
            removeDrag(session);
            return;
        }
        session.target = target;
    }

    public static void endDrag(Level level, Player player) {
        var session = DRAGS.remove(player.getUUID());
        if (session != null) removeJoint(session);
    }

    public static void tick() {
        DRAGS.entrySet().removeIf(entry -> !applyDrag(entry.getValue()));
    }

    private static boolean applyDrag(DragSession session) {
        if (!sessionValid(session) || !session.joint.isValid()) {
            removeJoint(session);
            return false;
        }

        // 目标点相对世界侧帧的位移，就是三个线性轴电机的期望位置（世界坐标、轴与帧1朝向一致）
        var targetRel = new Vector3d(JOMLConversion.toJOML(session.target)).sub(session.frame1Pos);
        var mass = 1.0;
        var massData = session.subLevel.getMassTracker();
        if (massData != null && massData.getMass() > 0.001) mass = massData.getMass();

        try {
            session.joint.setMotor(ConstraintJointAxis.LINEAR_X, targetRel.x, STIFFNESS * mass, DAMPING * mass, true, MAX_FORCE * mass);
            session.joint.setMotor(ConstraintJointAxis.LINEAR_Y, targetRel.y, STIFFNESS * mass, DAMPING * mass, true, MAX_FORCE * mass);
            session.joint.setMotor(ConstraintJointAxis.LINEAR_Z, targetRel.z, STIFFNESS * mass, DAMPING * mass, true, MAX_FORCE * mass);
        } catch (Exception e) {
            removeJoint(session);
            return false;
        }
        return true;
    }

    private static boolean sessionValid(DragSession session) {
        var player = session.player;
        var subLevel = session.subLevel;
        var level = subLevel.getLevel();
        if (!player.isAlive() || player.level() != level) return false;
        if (subLevel.isRemoved()) return false;
        var ragdoll = RagdollManager.get(session.ragdollUUID);
        if (ragdoll == null || !ragdoll.isAlive()) return false;

        var container = (ServerSubLevelContainer) ServerSubLevelContainer.getContainer(level);
        if (container == null || container.getSubLevel(subLevel.getUniqueId()) == null) return false;
        // 拖拽距离上限：拉太远视为脱手（绳子断了）
        return withinRange(level, player, session.anchor, MAX_RANGE);
    }

    private static void removeDrag(DragSession session) {
        DRAGS.remove(session.player.getUUID());
        removeJoint(session);
    }

    private static void removeJoint(DragSession session) {
        try {
            if (session.joint.isValid()) session.joint.remove();
        } catch (Exception e) {
            LOGGER.debug("移除拖拽约束失败：", e);
        }
    }

    private static boolean withinRange(ServerLevel level, ServerPlayer player, Vec3 anchor, double range) {
        return SableCompanion.INSTANCE.distanceSquaredWithSubLevels(
                level,
                JOMLConversion.toJOML(player.getEyePosition()),
                JOMLConversion.toJOML(anchor)
        ) <= range * range;
    }

    public static void reset() {
        DRAGS.values().forEach(RagdollDragManager::removeJoint);
        DRAGS.clear();
    }
}
