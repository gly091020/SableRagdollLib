package com.gly091020.SableRagdollLib.api.control;

import com.gly091020.SableRagdollLib.api.Ragdoll;
import com.gly091020.SableRagdollLib.block.AbstractPartBlockEntity;
import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 玩家对单个布娃娃的操控会话（落脚点行走模式）。
 * <p>
 * 不再使用"扶正/脚底固定/四肢摆动"那一堆电机约束。控制只有两部分：
 * <ul>
 *     <li>身体跟随：一条"世界 ↔ 身体"自由约束，X/Z 线性电机按客户端发来的移动输入
 *     （WASD 换算成世界方向）推进目标点，速度约 {@value #CONTROL_SPEED} 方块/秒，
 *     Y 交给重力；角轴用软回正电机保持直立（角静止帧每 tick 跟随 yaw，见下方"角轴经验"），移动时身体转向玩家摄像机方向。</li>
 *     <li>落脚点行走：每走 {@value #STEP_LENGTH} 方块切换一次支撑脚。算法算出下一个
 *     落脚点（身体目标位置 + 前进方向 × 半步长 + 侧向偏移），把新支撑脚的脚底中心用
 *     "电机回正"的世界↔腿约束绑上去——目标从脚当前位置逐渐收敛到落脚点，避免猛拽；
 *     另一只脚释放后随身体拖动，成为下一次的摆动脚。</li>
 * </ul>
 * <p><b>角轴经验（踩过的坑，勿改回）</b>：世界↔身体约束的角静止帧在建约束时确定，
 * 包含初始 yaw（模型出生朝向 -Z）。若 X/Z 直立电机目标固定为 0，它们会连 yaw 一起
 * 拉回初始朝向，与 Y 电机打架——症状：布娃娃总有面向 -Z 的趋势，面向 ±180° 时自转。
 * 因此每 tick 用 {@code setFrame2(groundPlotPos, rotationY(bodyYaw()))} 把角静止帧
 * 重锚定到当前 yaw，X/Z 只纠正俯仰/侧倾；Y 电机目标用"当前到摄像机的最短角"小步
 * （相对重锚定后的 0，误差永远很小，也绕开 ±π 缝）。不要再用相对初始 yaw 的
 * 绝对值/累加值当 Y 电机目标，也不要让 X/Z 电机目标包含 yaw 分量。
 * <p>
 * 会话开始如果身体是躺着的，先做一次性瞬移站直（旋转到只保留当前朝向 + 抬到脚底地面），
 * 然后锁定初始支撑脚。
 * <p>
 * 会话创建依赖 {@link RagdollPartRecognizerRegistry} 的六部位识别结果；
 * 识别失败或缺少身体时创建失败返回 {@code null}。
 */
public class RagdollControlSession {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** 控制移动速度（方块/秒） */
    private static final double CONTROL_SPEED = 0.5;
    /** 每游戏刻（50ms）的水平移动步长 */
    private static final double MOVE_STEP = CONTROL_SPEED * 0.5;
    /** 每刻允许的最大水平目标位移（安全上限） */
    private static final double MAX_STEP = 7 * 0.05;
    /** 身体跟随电机参数（×质量） */
    private static final double LINEAR_STIFFNESS = 90;
    private static final double LINEAR_DAMPING = 22;
    private static final double LINEAR_MAX_FORCE = 400;
    /** 身体直立回正电机参数（×质量）：角轴软回正，允许小幅倾斜 */
    private static final double ANGULAR_STIFFNESS = 3000;
    private static final double ANGULAR_DAMPING = 80;
    /** 身体回正电机最大扭矩上限，防止太僵硬 */
    private static final double ANGULAR_MAX_TORQUE = 1500;
    /** 身体转向摄像机方向的最大角速度（弧度/刻） */
    private static final double YAW_TURN_SPEED = 0.25;
    /** 支撑脚电机参数（×质量）：把脚底拉回落脚点，自动回正 */
    private static final double STANCE_STIFFNESS = 7000;
    private static final double STANCE_DAMPING = 100;
    /** 支撑脚电机最大力度上限，防止把肢体/身体拉飞 */
    private static final double STANCE_MAX_FORCE = 1200;
    /** 支撑脚电机目标每刻最多向落脚点收敛的距离（方块），避免换脚时一次性猛拽 */
    private static final double STANCE_TARGET_STEP = 0.3;
    /** 步长（方块）：身体每走这么多距离切换一次支撑脚 */
    private static final double STEP_LENGTH = 1;
    /** 落脚点相对身体前进方向的侧向偏移（左右各偏这么多） */
    private static final double STEP_SIDE = 0.2;
    /** 脚底中心相对腿质心的向下偏移（半格） */
    private static final double FOOT_OFFSET = 0.4;  // fixme:改成动态识别
    /** 会话开始倾斜超过该角度就瞬移站直（度） */
    private static final double STAND_SNAP_THRESHOLD_DEG = 20;
    private static final double STAND_SNAP_THRESHOLD = Math.toRadians(STAND_SNAP_THRESHOLD_DEG);
    /** 站直抬升上限，防止异常坐标把布娃娃抬飞 */
    private static final double MAX_LIFT = 2.0;

    private final ServerPlayer player;
    private final Ragdoll ragdoll;
    private final ServerSubLevelContainer container;
    private final ServerSubLevel body;
    private final ServerSubLevel leftLeg;
    private final ServerSubLevel rightLeg;

    /** 世界↔身体自由约束：X/Z 线性电机驱动移动，Y 交给重力 */
    private GenericConstraintHandle groundJoint;
    /** 地面约束世界侧锚点（会话开始时固定），电机目标相对它计算 */
    private final Vector3d groundFramePos = new Vector3d();
    /** 身体质心的 plotgrid 坐标（约束部件侧锚点所在空间） */
    private final Vector3d groundPlotPos = new Vector3d();
    /** 当前目标位置（随输入推进） */
    private final Vector3d lastTarget = new Vector3d();

    /** 客户端输入：世界坐标下的水平移动方向（未归一化） */
    private float inputMoveX;
    private float inputMoveZ;
    private boolean inputMoving;
    /** 客户端输入：玩家摄像机 yaw（世界角度，度） */
    private float inputYaw;
    /** 已走过的距离，达到 STEP_LENGTH 切换一次支撑脚 */
    private double stepProgress;
    /** 当前支撑脚与它的回正约束（自由约束 + 线性电机） */
    private ServerSubLevel stanceLeg;
    private GenericConstraintHandle stanceJoint;
    /** 支撑脚约束的世界侧帧（落脚点，主世界坐标），电机目标相对它计算 */
    private final Vector3d stanceFramePos = new Vector3d();
    /** 支撑脚电机目标（相对世界侧帧的位移），每刻向 0 收敛 */
    private final Vector3d stanceTargetRel = new Vector3d();
    /** 最近一次移动方向（世界坐标，归一化），停下后保留用于迈步 */
    private final Vector3d lastDir = new Vector3d(0, 0, 1);
    private boolean disposed;

    private RagdollControlSession(ServerPlayer player, Ragdoll ragdoll, ServerSubLevelContainer container,
                                  ServerSubLevel body, ServerSubLevel leftLeg, ServerSubLevel rightLeg) {
        this.player = player;
        this.ragdoll = ragdoll;
        this.container = container;
        this.body = body;
        this.leftLeg = leftLeg;
        this.rightLeg = rightLeg;
    }

    /**
     * 创建操控会话。按识别结果定位身体与双腿，失败返回 {@code null}（不产生任何副作用）。
     */
    public static RagdollControlSession create(ServerPlayer player, Ragdoll ragdoll) {
        if (player == null || ragdoll == null || !ragdoll.isAlive()) {
            return null;
        }
        var subLevels = ragdoll.getSublevels();
        if (subLevels.isEmpty()) {
            return null;
        }
        var container = (ServerSubLevelContainer) ServerSubLevelContainer.getContainer(subLevels.getFirst().getLevel());
        if (container == null) {
            return null;
        }

        AbstractPartBlockEntity bodyBE = null;
        for (var sub : subLevels) {
            if (sub.getPlot().getEmbeddedLevelAccessor().getBlockEntity(BlockPos.ZERO) instanceof AbstractPartBlockEntity be
                    && be.getPartData() != null && be.getPartData().isMain()) {
                bodyBE = be;
                break;
            }
        }
        if (bodyBE == null) {
            return null;
        }
        ResourceLocation defId = bodyBE.getPartData().defFile();
        Map<PartRole, String> roles = RagdollPartRecognizerRegistry.recognize(defId);
        String bodyName = roles.get(PartRole.BODY);
        if (bodyName == null) {
            return null;
        }

        Map<String, ServerSubLevel> byName = new HashMap<>();
        for (var sub : subLevels) {
            if (sub.getPlot().getEmbeddedLevelAccessor().getBlockEntity(BlockPos.ZERO) instanceof AbstractPartBlockEntity be
                    && be.getPartData() != null) {
                byName.put(be.getPartData().partName(), sub);
            }
        }
        ServerSubLevel body = byName.get(bodyName);
        if (body == null || body.isRemoved()) {
            return null;
        }

        RagdollControlSession session = new RagdollControlSession(
                player, ragdoll, container, body,
                byName.get(roles.get(PartRole.LEFT_LEG)),
                byName.get(roles.get(PartRole.RIGHT_LEG)));
        session.standUpIfTilted();
        if (!session.initGround()) {
            session.dispose();
            return null;
        }
        session.lockInitialStance();
        return session;
    }

    public void tick() {
        if (!isValid()) {
            dispose();
            return;
        }
        if (stanceJoint != null && !stanceJoint.isValid()) {
            releaseStance();
        }
        updateStep();
        applyStanceMotors();
        updateGround();
    }

    /**
     * 客户端输入入口：由服务端数据包处理器调用。
     */
    public void updateInput(float moveX, float moveZ, boolean moving, float yaw) {
        this.inputMoveX = moveX;
        this.inputMoveZ = moveZ;
        this.inputMoving = moving;
        this.inputYaw = yaw;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public boolean isValid() {
        if (disposed) {
            return false;
        }
        if (!player.isAlive() || player.level() != body.getLevel()) {
            return false;
        }
        if (!ragdoll.isAlive() || !ragdoll.isLoad()) {
            return false;
        }
        if (groundJoint == null || !groundJoint.isValid() || body.isRemoved()) {
            return false;
        }
        return true;
    }

    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        releaseStance();
        removeJoint(groundJoint);
        groundJoint = null;
    }

    /** 创建世界↔身体跟随约束：角轴软回正电机保持直立，目标点从布娃娃当前世界位置开始。 */
    private boolean initGround() {
        var pose = body.logicalPose();
        // plotgrid 坐标（物理结构实际存放的空间）：质心 rotationPoint 位于本部件的 plot 内
        var plotCenter = new Vector3d(pose.rotationPoint());
        // 同一锚点投影回主世界坐标：世界侧约束帧必须位于 plotgrid 之外
        var worldCenter = pose.transformPosition(plotCenter, new Vector3d());
        GenericConstraintHandle joint;
        try {
            joint = container.physicsSystem().getPipeline().addConstraint(
                    null, body,
                    new GenericConstraintConfiguration(worldCenter, plotCenter, new Quaterniond(), new Quaterniond(), Set.of()));
        } catch (Exception e) {
            LOGGER.debug("创建身体跟随约束失败：", e);
            return false;
        }
        if (joint == null || !joint.isValid()) {
            return false;
        }
        groundJoint = joint;
        groundFramePos.set(worldCenter);
        groundPlotPos.set(plotCenter);
        lastTarget.set(worldCenter);
        return true;
    }

    /**
     * 会话开始如果身体是躺着的：把整个布娃娃绕身体中心旋转到"只保留当前朝向"的直立姿态，
     * 并抬升到旋转前的最低点高度（脚底接触的地面，不用高度图，避免室内被抬到屋顶）。
     */
    private void standUpIfTilted() {
        if (currentTilt() <= STAND_SNAP_THRESHOLD) {
            return;
        }
        double groundRef = lowestPointY();
        Quaterniond bodyOri = new Quaterniond(body.logicalPose().orientation());
        Vector3d euler = bodyOri.getEulerAnglesXYZ(new Vector3d());
        Quaterniond target = new Quaterniond().rotationY(euler.y);
        Quaterniond delta = bodyOri.conjugate().mul(target);
        ragdoll.rotate(delta);

        double lowest = lowestPointY();
        double lift = Math.min(MAX_LIFT, Math.max(0, groundRef + 0.05 - lowest));
        if (lift > 0) {
            ragdoll.move(new Vector3d(0, lift, 0), false);
        }
    }

    private double currentTilt() {
        Vector3d up = body.logicalPose().orientation().transform(new Vector3d(0, 1, 0), new Vector3d());
        return up.angle(new Vector3d(0, 1, 0));
    }

    /** 身体当前朝向（世界 yaw，Minecraft 约定：0 = +Z）。 */
    private double bodyYaw() {
        Vector3d forward = body.logicalPose().orientation().transform(new Vector3d(0, 0, 1), new Vector3d());
        return Math.atan2(-forward.x, forward.z);
    }

    /** 所有部件最低点的世界 Y（粗略减半格）。 */
    private double lowestPointY() {
        double min = Double.MAX_VALUE;
        for (ServerSubLevel sub : ragdoll.getSublevels()) {
            var pose = sub.logicalPose();
            double y = pose.transformPosition(pose.rotationPoint(), new Vector3d()).y - 0.5;
            min = Math.min(min, y);
        }
        return min == Double.MAX_VALUE ? 0 : min;
    }

    private boolean isMoving() {
        return inputMoving && (inputMoveX != 0 || inputMoveZ != 0);
    }

    private void updateGround() {
        if (groundJoint == null || !groundJoint.isValid()) {
            return;
        }
        Vector3d target = new Vector3d(lastTarget);
        if (isMoving()) {
            double len = Math.sqrt(inputMoveX * inputMoveX + inputMoveZ * inputMoveZ);
            if (len > 0.0001) {
                target.x += inputMoveX / len * MOVE_STEP;
                target.z += inputMoveZ / len * MOVE_STEP;
            }
        }
        // 安全上限：目标位移不超过 MAX_STEP
        double dx = target.x - lastTarget.x;
        double dz = target.z - lastTarget.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > MAX_STEP) {
            double scale = MAX_STEP / dist;
            target.x = lastTarget.x + dx * scale;
            target.z = lastTarget.z + dz * scale;
        }
        lastTarget.set(target);

        // 每刻把角约束静止帧重锚定到身体当前 yaw：X/Z 直立电机只纠正俯仰/侧倾，
        // 不再把 yaw 拉回初始朝向（否则会和 Y 电机打架、出现 -Z 趋势）。
        // Y 电机目标取"当前到摄像机的最短角"（相对重锚定后的 0，永远小角度，也绕开 ±π 缝）。
        // 坑（详见类注释"角轴经验"）：不要改成"相对初始 yaw 的累加/绝对值"当 Y 目标，跨 ±π 会整圈自转。
        double yawTarget = 0;
        if (isMoving()) {
            double desiredYaw = Math.toRadians(inputYaw);
            double diff = Math.atan2(Math.sin(desiredYaw - bodyYaw()), Math.cos(desiredYaw - bodyYaw()));
            double step = Math.copySign(Math.min(YAW_TURN_SPEED, Math.abs(diff)), diff);
            yawTarget = -step;
        }
        double mass = massOf(body);
        try {
            groundJoint.setFrame2(groundPlotPos, new Quaterniond().rotationY(bodyYaw()));
            groundJoint.setMotor(ConstraintJointAxis.LINEAR_X, target.x - groundFramePos.x,
                    LINEAR_STIFFNESS * mass, LINEAR_DAMPING * mass, true, LINEAR_MAX_FORCE * mass);
            groundJoint.setMotor(ConstraintJointAxis.LINEAR_Z, target.z - groundFramePos.z,
                    LINEAR_STIFFNESS * mass, LINEAR_DAMPING * mass, true, LINEAR_MAX_FORCE * mass);
            // Y 轴不设电机：交给重力
            // 角轴软回正（静止帧已跟随 yaw，X/Z 只纠正俯仰/侧倾）
            groundJoint.setMotor(ConstraintJointAxis.ANGULAR_X, 0,
                    ANGULAR_STIFFNESS * mass, ANGULAR_DAMPING * mass, true, ANGULAR_MAX_TORQUE * mass);
            groundJoint.setMotor(ConstraintJointAxis.ANGULAR_Y, yawTarget,
                    ANGULAR_STIFFNESS * mass, ANGULAR_DAMPING * mass, true, ANGULAR_MAX_TORQUE * mass);
            groundJoint.setMotor(ConstraintJointAxis.ANGULAR_Z, 0,
                    ANGULAR_STIFFNESS * mass, ANGULAR_DAMPING * mass, true, ANGULAR_MAX_TORQUE * mass);
        } catch (Exception e) {
            LOGGER.debug("驱动身体移动电机失败：", e);
        }
    }

    /**
     * 落脚点行走：身体每走 STEP_LENGTH 方块切换一次支撑脚。
     * 新支撑脚（另一条腿）被锁定到算法算出的下一个落脚点，旧支撑脚释放。
     */
    private void updateStep() {
        if (isMoving()) {
            double len = Math.sqrt(inputMoveX * inputMoveX + inputMoveZ * inputMoveZ);
            if (len > 0.0001) {
                lastDir.set(inputMoveX / len, 0, inputMoveZ / len);
            }
            stepProgress += MOVE_STEP;
            if (stepProgress >= STEP_LENGTH) {
                stepProgress -= STEP_LENGTH;
                doStep();
            }
        } else {
            stepProgress = 0;
        }
    }

    /** 切换支撑脚：把另一条腿锁定到下一个落脚点。 */
    private void doStep() {
        ServerSubLevel next = stanceLeg == leftLeg ? rightLeg : leftLeg;
        if (next == null) {
            return;
        }
        lockFoot(next, nextFoothold(next));
    }

    /**
     * 算法确定下一个落脚点：身体目标位置 + 身体当前朝向 × 半步长 + 侧向偏移，
     * 高度取所在方块顶面（世界坐标紧贴方块，避免悬空或陷入）。
     * 迈步方向用身体当前朝向（与 yaw 跟随一致），而不是瞬时输入方向，
     * 否则转向时脚会先于身体转向。
     */
    private Vector3d nextFoothold(ServerSubLevel leg) {
        // 直接用身体当前实测朝向作为迈步方向（不用累加目标，避免与实际朝向不同步）
        double facingYaw = bodyYaw();
        Vector3d forward = new Vector3d(-Math.sin(facingYaw), 0, Math.cos(facingYaw));
        // 朝向的左方向（世界坐标）
        Vector3d side = new Vector3d(forward.z, 0, -forward.x);
        // 该腿实际在身体左侧还是右侧（按腿质心在左方向上的投影，不依赖命名/渲染镜像）
        double sideSign = legSideSign(leg, side);
        Vector3d foothold = new Vector3d(lastTarget);
        foothold.x += forward.x * STEP_LENGTH * 0.5 + side.x * sideSign * STEP_SIDE;
        foothold.z += forward.z * STEP_LENGTH * 0.5 + side.z * sideSign * STEP_SIDE;
        // 脚底贴方块：向下探测目标位置的实际地面顶面（不用高度图，避免室内取到屋顶）
        foothold.y = groundTopAt(foothold.x, foothold.z, footWorld(leg).y);
        return foothold;
    }

    /**
     * 判断腿相对身体朝向的左右：把腿质心投影到"朝向的左方向"上，
     * 正 = 在身体左侧。用实际物理位置判断，与模型命名左右、渲染镜像无关。
     */
    private double legSideSign(ServerSubLevel leg, Vector3d side) {
        var bodyPose = body.logicalPose();
        var bodyPos = bodyPose.transformPosition(bodyPose.rotationPoint(), new Vector3d());
        var legPose = leg.logicalPose();
        var legPos = legPose.transformPosition(legPose.rotationPoint(), new Vector3d());
        double dx = legPos.x - bodyPos.x;
        double dz = legPos.z - bodyPos.z;
        return dx * side.x + dz * side.z >= 0 ? 1 : -1;
    }

    /**
     * 向下扫描目标 X/Z 脚下第一个有碰撞箱的普通方块，返回其顶面 Y（世界坐标）。
     * 不用 level.clip：Sable 的 clip_overwrite mixin 会让 clip 命中布娃娃自身的物理结构，
     * 并把子维度上的命中点换算回世界坐标，导致探测到身体/腿的高度；
     * 这里用 getBlockState 逐格下扫，只认主世界地形方块。
     */
    private double groundTopAt(double x, double z, double refY) {
        var level = body.getLevel();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int topY = (int) Math.floor(refY + 1.0);
        int bottomY = (int) Math.floor(refY - 24.0);
        for (int y = topY; y >= bottomY; y--) {
            pos.set(x, y, z);
            var state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            var shape = state.getCollisionShape(level, pos);
            if (shape.isEmpty()) {
                continue;
            }
            return y + shape.max(Direction.Axis.Y);
        }
        return refY;
    }

    /** 会话开始把第一只脚（优先左腿）锁定在它当前脚底位置，作为初始支撑脚。 */
    private void lockInitialStance() {
        ServerSubLevel leg = leftLeg != null ? leftLeg : rightLeg;
        if (leg != null) {
            Vector3d foot = footWorld(leg);
            foot.y = groundTopAt(foot.x, foot.z, foot.y);
            lockFoot(leg, foot);
        }
    }

    /** 把腿的脚底中心约束到落脚点：电机目标从脚当前位置逐渐收敛，线性电机自动回正（最大力度受限）。 */
    private void lockFoot(ServerSubLevel leg, Vector3d foothold) {
        releaseStance();
        var pose = leg.logicalPose();
        // 脚底中心在腿 plot 内的坐标（当前姿态下脚底世界位置对应的 plotgrid 点）
        var foot = footWorld(leg);
        var footPlot = pose.transformPositionInverse(foot, new Vector3d());
        GenericConstraintHandle joint;
        try {
            joint = container.physicsSystem().getPipeline().addConstraint(
                    null, leg,
                    new GenericConstraintConfiguration(
                            foothold, footPlot,
                            new Quaterniond(), new Quaterniond(),
                            Set.of()));
        } catch (Exception e) {
            LOGGER.debug("创建落脚点约束失败：", e);
            return;
        }
        if (joint == null || !joint.isValid()) {
            return;
        }
        stanceJoint = joint;
        stanceLeg = leg;
        // 世界侧帧固定在落脚点；电机目标从脚当前的位置开始，每刻向 0 收敛（逐渐落回落脚点）
        stanceFramePos.set(foothold);
        stanceTargetRel.set(foot).sub(foothold);
        applyStanceMotors();
    }

    /**
     * 支撑脚电机：目标每刻向 0 收敛（脚底逐渐自动回正到落脚点），
     * 带最大力度限制，避免换脚瞬间猛拽把肢体/身体拉飞。
     */
    private void applyStanceMotors() {
        if (stanceJoint == null || !stanceJoint.isValid() || stanceLeg == null) {
            return;
        }
        // 目标向 0 收敛：每刻最多靠近 STANCE_TARGET_STEP 方块，换脚不会瞬间猛拽
        double len = stanceTargetRel.length();
        if (len <= STANCE_TARGET_STEP) {
            stanceTargetRel.set(0, 0, 0);
        } else {
            double scale = 1.0 - STANCE_TARGET_STEP / len;
            stanceTargetRel.mul(scale);
        }
        double mass = massOf(stanceLeg);
        try {
            stanceJoint.setMotor(ConstraintJointAxis.LINEAR_X, stanceTargetRel.x,
                    STANCE_STIFFNESS * mass, STANCE_DAMPING * mass,
                    true, STANCE_MAX_FORCE * mass);
            stanceJoint.setMotor(ConstraintJointAxis.LINEAR_Y, stanceTargetRel.y,
                    STANCE_STIFFNESS * mass, STANCE_DAMPING * mass,
                    true, STANCE_MAX_FORCE * mass);
            stanceJoint.setMotor(ConstraintJointAxis.LINEAR_Z, stanceTargetRel.z,
                    STANCE_STIFFNESS * mass, STANCE_DAMPING * mass,
                    true, STANCE_MAX_FORCE * mass);
        } catch (Exception e) {
            LOGGER.debug("设置支撑脚电机失败：", e);
        }
    }

    /** 释放当前支撑脚的锁定。 */
    private void releaseStance() {
        removeJoint(stanceJoint);
        stanceJoint = null;
        stanceLeg = null;
    }

    /** 腿的脚底中心世界位置：质心向下偏移半格。 */
    private Vector3d footWorld(ServerSubLevel leg) {
        var pose = leg.logicalPose();
        var com = pose.transformPosition(pose.rotationPoint(), new Vector3d());
        return com.sub(0, FOOT_OFFSET, 0, new Vector3d());
    }

    private static double massOf(ServerSubLevel subLevel) {
        var tracker = subLevel.getMassTracker();
        if (tracker != null && tracker.getMass() > 0.001) {
            return tracker.getMass();
        }
        return 1.0;
    }

    private static void removeJoint(GenericConstraintHandle joint) {
        if (joint == null) {
            return;
        }
        try {
            if (joint.isValid()) {
                joint.remove();
            }
        } catch (Exception e) {
            LOGGER.debug("移除控制约束失败：", e);
        }
    }
}