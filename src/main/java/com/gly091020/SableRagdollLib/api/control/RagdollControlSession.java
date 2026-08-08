package com.gly091020.SableRagdollLib.api.control;

import com.gly091020.SableRagdollLib.api.Ragdoll;
import com.gly091020.SableRagdollLib.network.ClientboundRagdollGrabRayPacket;
import com.gly091020.SableRagdollLib.block.AbstractPartBlockEntity;
import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 感谢哔哩哔哩 uid49871902的用户（每天一瓶降压药）提供的建议
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
 *     <li>跳跃：空格键（客户端随输入包上报）按上升沿触发，释放支撑脚并给身体一个
 *     向上的速度（v = sqrt(2*g*h)），滞空/坠落期间暂停迈步（脚底离地即停，
 *     避免支撑脚锁在半空拖慢下落）；空中仍可水平转向/移动。</li>
 *     <li>抓取：左 Alt 键。按住时双手一起抬起——目标按各模型手臂自身长度算，
 *     手臂转到与身体垂直（水平前伸），方向由身体↔手臂约束的 motor target
 *     控制（内力不拽飞身体），不写死高度；松开瞬间从手掌底部中心沿"手的正方向"
 *     射线探测（最远 {@value #GRAB_REACH} 格，与玩家视线无关），命中主世界方块
 *     或其它物理结构就用软电机把两只手掌拉到命中点两侧（左手偏身体左侧、右手
 *     偏右侧，不挤在一起）；抓到物理结构时还带角轴同步，物品跟着手臂一起转。
 *     抬手电机继续尝试保持抬手姿势（物品被拎在手上，不会耷拉拖地）；没命中或
 *     命中点太低（地面/脚下）就不抓，手臂放下；再次按下恢复，手臂自然垂落。
 *     只有按住 Alt 瞄准期间客户端会画出探测射线与选中的抓取位置。</li>
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
    private static final double CONTROL_SPEED = 1;
    /** 每游戏刻（50ms）的水平移动步长 */
    private static final double MOVE_STEP = CONTROL_SPEED * 0.5;
    /** 每刻允许的最大水平目标位移（安全上限） */
    private static final double MAX_STEP = 7 * 0.05;
    /** 目标点与身体实际位置允许的最大水平滞后（方块）：被墙挡住时目标不再前移，破墙后不会猛拽 */
    private static final double MAX_TARGET_LAG = 0.5;
    /** 跳跃高度（方块）：起跳速度 v = sqrt(2*g*h) */
    private static final double JUMP_HEIGHT = 50;
    /** 跳跃后暂停迈步的刻数（约等于滞空时间），避免空中把脚锁在半空 */
    private static final int JUMP_AIR_TICKS = 5;
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
    private static final double STANCE_STIFFNESS = 5000;
    private static final double STANCE_DAMPING = 100;
    /** 支撑脚电机最大力度上限，防止把肢体/身体拉飞 */
    private static final double STANCE_MAX_FORCE = 1200;
    /** 支撑脚电机目标每刻最多向落脚点收敛的距离（方块），避免换脚时一次性猛拽 */
    private static final double STANCE_TARGET_STEP = 0.3;
    /** 抓取电机参数（×质量）：手掌向抓取点回正。力度刻意调小：手只是"抬起来"，
     *  不是飞出去——力太大经手臂关节传回身体会把整个布娃娃拽飞（踩过的坑） */
    private static final double GRAB_STIFFNESS = 1500;
    private static final double GRAB_DAMPING = 60;
    /** 抓取电机最大力度上限，防止把手臂/身体拽飞 */
    private static final double GRAB_MAX_FORCE = 1000;
    /** 抓取电机目标每刻最多向抓取点收敛的距离（方块），手是逐渐伸出去的 */
    private static final double GRAB_TARGET_STEP = 0.15;
    /** 抓取阶段（松开 Alt 后）目标收敛速度（方块/刻）：手快速落到物品两侧 */
    private static final double GRAB_HOLD_STEP = 0.5;
    /** 两只手抓同一个物品时的横向间距（方块）：左手偏身体左侧、右手偏右侧，不挤到一起 */
    private static final double GRAB_HAND_SPACING = 0.35;
    /** 抓取角轴同步电机参数（×质量）：让物品跟着手臂转（只有抓到物理结构时启用） */
    private static final double GRAB_ANGULAR_STIFFNESS = 800;
    private static final double GRAB_ANGULAR_DAMPING = 60;
    private static final double GRAB_ANGULAR_MAX_TORQUE = 200;
    /** 抓取探测距离（方块）：从手掌底部中心沿手的正方向探测 */
    private static final double GRAB_REACH = 0.5;
    /** 射线起点往手后方缩进（方块）：手贴着物体时也能命中它，而不是从表面正中央漏过去 */
    private static final double GRAB_RAY_INSET = 0.05;
    /** 命中点低于手掌超过该值（方块）就视为地面/脚下，不算抓取目标 */
    private static final double GRAB_MIN_HEIGHT_DROP = 0.4;
    /** 步长（方块）：身体每走这么多距离切换一次支撑脚 */
    private static final double STEP_LENGTH = 1.5;
    /** 落脚点相对身体前进方向的侧向偏移（左右各偏这么多） */
    private static final double STEP_SIDE = 0.1;
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
    private final ServerSubLevel leftArm;
    private final ServerSubLevel rightArm;

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
    /** 上一刻是否在移动（停止移动后释放支撑脚关节） */
    private boolean wasMoving;
    /** 客户端输入：是否按住跳跃键（空格） */
    private boolean inputJumping;
    /** 上一刻跳跃键状态：上升沿（松开→按下）触发一次跳跃 */
    private boolean prevJumpHeld;
    /** 跳跃后剩余的空中刻数：空中暂停迈步 */
    private int jumpAirTicks;
    /** 客户端输入：玩家摄像机俯仰角（度，-90 上 / +90 下）。
     *  抓取方向已改为按手的正方向，此字段暂未使用（保留备用）。 */
    private float inputPitch;
    /** 客户端输入：是否按住抓取键（左 Alt） */
    private boolean inputGrab;
    /** 上一刻抓取键状态：用于上升沿/下降沿切换状态机 */
    private boolean prevGrabHeld;
    /** 抓取状态机：IDLE=空闲，REACHING=按住 Alt 双手前伸，HOLDING=松开 Alt 后锁定抓取 */
    private static final int GRAB_IDLE = 0;
    private static final int GRAB_REACHING = 1;
    private static final int GRAB_HOLDING = 2;
    private int grabState = GRAB_IDLE;
    /** 两只抓取手（左右各一，缺失时只用存在的那只） */
    private ServerSubLevel grabHandA;
    private ServerSubLevel grabHandB;
    /** 抬手约束：身体 ↔ 手臂（自由约束 + 线性电机），抓取后也继续尝试保持抬手姿势 */
    private GenericConstraintHandle raiseJointA;
    private GenericConstraintHandle raiseJointB;
    /** 抬手电机目标（约束帧坐标 = 身体局部轴），每刻向 raiseFinalRel 收敛 */
    private final Vector3d raiseTargetRelA = new Vector3d();
    private final Vector3d raiseTargetRelB = new Vector3d();
    /** 抬手最终目标（身体局部坐标：手掌抬到与肩同高、手臂水平前伸），每只手臂按自身长度算 */
    private final Vector3d raiseFinalRelA = new Vector3d();
    private final Vector3d raiseFinalRelB = new Vector3d();
    /** 抓取约束：手 ↔ 抓取点（自由约束 + 线性电机），把物品固定在手上 */
    private GenericConstraintHandle grabJointA;
    private GenericConstraintHandle grabJointB;
    /** 抓取电机目标（手掌相对抓取点的位移），每刻向 grabFinalRel 收敛 */
    private final Vector3d grabTargetRelA = new Vector3d();
    private final Vector3d grabTargetRelB = new Vector3d();
    /** 抓取最终目标（约束帧坐标）：手掌停在物品两侧的横向偏移（左手偏左、右手偏右） */
    private final Vector3d grabFinalRelA = new Vector3d();
    private final Vector3d grabFinalRelB = new Vector3d();
    /** 该手的抓取目标是否为物理结构（结构抓取才开角轴同步，让物品跟着手臂转） */
    private boolean grabIsStructureA;
    private boolean grabIsStructureB;
    /** 抓取射线渲染（服务端算好发给客户端）：起点、终点（命中点或射线末端）、是否选中 */
    private final Vector3d debugRayOrigin = new Vector3d();
    private final Vector3d debugRayEnd = new Vector3d();
    private boolean debugRayHit;
    /** 客户端当前是否显示抓取射线（离开瞄准状态时发一次清除） */
    private boolean debugRayVisible;
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
                                  ServerSubLevel body, ServerSubLevel leftLeg, ServerSubLevel rightLeg,
                                  ServerSubLevel leftArm, ServerSubLevel rightArm) {
        this.player = player;
        this.ragdoll = ragdoll;
        this.container = container;
        this.body = body;
        this.leftLeg = leftLeg;
        this.rightLeg = rightLeg;
        this.leftArm = leftArm;
        this.rightArm = rightArm;
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

        // 左右手不信任名称识别：有的模型部件命名左右反了，按实际物理位置重判
        ServerSubLevel[] arms = resolveArmsBySide(
                body,
                byName.get(roles.get(PartRole.LEFT_ARM)),
                byName.get(roles.get(PartRole.RIGHT_ARM)));
        RagdollControlSession session = new RagdollControlSession(
                player, ragdoll, container, body,
                byName.get(roles.get(PartRole.LEFT_LEG)),
                byName.get(roles.get(PartRole.RIGHT_LEG)),
                arms[0], arms[1]);
        session.standUpIfTilted();
        if (!session.initGround()) {
            session.dispose();
            return null;
        }
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
        if ((raiseJointA != null && !raiseJointA.isValid()) || (raiseJointB != null && !raiseJointB.isValid())
                || (grabJointA != null && !grabJointA.isValid()) || (grabJointB != null && !grabJointB.isValid())) {
            releaseGrab();
        }
        // 跳跃：空格上升沿（松开→按下）触发一次，按住不会连跳；空中（滞空计数未结束）不可再跳
        if (jumpAirTicks == 0 && inputJumping && !prevJumpHeld) {
            doJump();
        }
        prevJumpHeld = inputJumping;
        if (jumpAirTicks > 0) {
            jumpAirTicks--;
        }
        // 抓取状态机：
        //   IDLE --Alt 按下--> REACHING（双手沿视线前伸）
        //   REACHING --Alt 松开--> HOLDING（抓取视线方向的方块/结构，保持伸手姿势）
        //   HOLDING --Alt 按下--> IDLE（恢复，手臂自然垂落）
        if (inputGrab && !prevGrabHeld) {
            if (grabState == GRAB_IDLE) {
                startReach();
            } else if (grabState == GRAB_HOLDING) {
                releaseGrab();
            }
        } else if (!inputGrab && prevGrabHeld && grabState == GRAB_REACHING) {
            finishGrab();
        }
        prevGrabHeld = inputGrab;
        // 只有第一次按住 Alt（瞄准、还没松开）期间渲染抓取射线；
        // 松开进入抓取或恢复后发一次清除
        boolean wantRay = grabState == GRAB_REACHING;
        if (wantRay) {
            Vector3d center = handCenter();
            if (center != null) {
                computeGrabTarget(center);
                sendGrabRay(true);
            }
        } else if (debugRayVisible) {
            sendGrabRay(false);
        }
        updateStep();
        applyStanceMotors();
        applyGrabMotors();
        updateGround();
    }

    /**
     * 客户端输入入口：由服务端数据包处理器调用。
     * moveX/moveZ 为世界坐标下的水平移动方向，yaw/pitch 为玩家摄像机朝向
     * （世界角度，度），jumping 为跳跃键按住状态，grab 为抓取键（左 Alt）按住状态。
     */
    public void updateInput(float moveX, float moveZ, boolean moving, float yaw, float pitch, boolean jumping, boolean grab) {
        this.inputMoveX = moveX;
        this.inputMoveZ = moveZ;
        this.inputMoving = moving;
        this.inputYaw = yaw;
        this.inputPitch = pitch;
        this.inputJumping = jumping;
        this.inputGrab = grab;
    }

    /**
     * 触发跳跃：释放支撑脚（脚跟着身体一起跳），给身体一个向上的速度，
     * 并暂停空中迈步，等落地后恢复。
     * 起跳速度 v = sqrt(2*g*h)，g 取该维度实际重力（Sable 默认 11 m/s²）。
     */
    private void doJump() {
        releaseStance();
        double g = -DimensionPhysicsData.getGravity(body.getLevel()).y;
        if (g <= 0.1) {
            g = 11.0;
        }
        double v = Math.sqrt(2.0 * g * JUMP_HEIGHT);
        try {
            container.physicsSystem().getPhysicsHandle(body)
                    .addLinearAndAngularVelocity(new Vector3d(0, v, 0), new Vector3d());
        } catch (Exception e) {
            LOGGER.debug("施加跳跃速度失败：", e);
        }
        jumpAirTicks = JUMP_AIR_TICKS;
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
        releaseGrab();
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

    /**
     * 身体是否处于空中：脚底离地面超过 0.7 格就算空中（跳跃滞空、走空坠落）。
     * 空中会暂停迈步，避免支撑脚锁在半空影响 Y 轴下落（走路时下落减速的根因）。
     */
    private boolean isAirborne() {
        var bodyAnchor = body.logicalPose().transformPosition(groundPlotPos, new Vector3d());
        double refY = lowestPointY();
        double ground = rawGroundTopAt(bodyAnchor.x, bodyAnchor.z, refY);
        return Double.isNaN(ground) || refY - ground > 0.7;
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
        // 滞后上限：目标点离身体实际位置太远（被墙等挡住时目标会一直前移）就拉回，
        // 否则破墙后身体会被积累的目标误差猛拽过去。
        var bodyAnchor = body.logicalPose().transformPosition(groundPlotPos, new Vector3d());
        double lagX = target.x - bodyAnchor.x;
        double lagZ = target.z - bodyAnchor.z;
        double lag = Math.sqrt(lagX * lagX + lagZ * lagZ);
        if (lag > MAX_TARGET_LAG) {
            double scale = MAX_TARGET_LAG / lag;
            target.x = bodyAnchor.x + lagX * scale;
            target.z = bodyAnchor.z + lagZ * scale;
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
            wasMoving = true;
            double len = Math.sqrt(inputMoveX * inputMoveX + inputMoveZ * inputMoveZ);
            if (len > 0.0001) {
                lastDir.set(inputMoveX / len, 0, inputMoveZ / len);
            }
            if (jumpAirTicks > 0 || isAirborne()) {
                // 空中不迈步（跳跃滞空或走空坠落），并解锁支撑脚锚点：
                // 否则身体下落时会被还锚在悬崖边/半空的脚拽住（下落减速的根因）
                stepProgress = 0;
                releaseStance();
                return;
            }
            stepProgress += MOVE_STEP;
            if (stepProgress >= STEP_LENGTH) {
                stepProgress -= STEP_LENGTH;
                doStep();
            }
        } else {
            stepProgress = 0;
            if (stanceJoint != null && isAirborne()) {
                // 没按移动键但脚底离地（被推下悬崖等）：同样解锁支撑脚锚点
                releaseStance();
            }
            if (wasMoving) {
                // 停止移动后删除支撑脚关节：站立时不再把脚绑在落脚点上
                wasMoving = false;
                releaseStance();
            }
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
        // 迈步方向用身体实际移动方向（速度方向）：倒退/斜走时脚要往移动方向迈，
        // 而不是身体朝向——否则支撑脚锁在身体前方，会把倒退挡住。
        // 不用瞬时输入方向直接算：转向时输入方向瞬间变，脚会先于身体转向。
        // 速度太小时（刚起步/被墙挡）退回输入方向 lastDir。
        Vector3d stepDir = actualMoveDir();
        // 朝向的左方向（世界坐标）：左右脚的侧向偏移仍相对身体朝向
        double facingYaw = bodyYaw();
        Vector3d forward = new Vector3d(-Math.sin(facingYaw), 0, Math.cos(facingYaw));
        Vector3d side = new Vector3d(forward.z, 0, -forward.x);
        // 该腿实际在身体左侧还是右侧（按腿质心在左方向上的投影，不依赖命名/渲染镜像）
        double sideSign = legSideSign(leg, side);
        Vector3d foothold = new Vector3d(lastTarget);
        foothold.x += stepDir.x * STEP_LENGTH * 0.5 + side.x * sideSign * STEP_SIDE;
        foothold.z += stepDir.z * STEP_LENGTH * 0.5 + side.z * sideSign * STEP_SIDE;
        // 脚底贴方块：向下探测目标位置的实际地面顶面（不用高度图，避免室内取到屋顶）
        foothold.y = groundTopAt(foothold.x, foothold.z, footWorld(leg).y);
        return foothold;
    }

    /**
     * 身体当前实际水平移动方向（世界坐标，归一化）。
     * 用身体部件的物理速度方向；速度太小（刚起步/被挡）时退回输入方向 lastDir。
     */
    private Vector3d actualMoveDir() {
        var vel = body.latestLinearVelocity;
        double h = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        if (h > 0.02) {
            return new Vector3d(vel.x / h, 0, vel.z / h);
        }
        double len = Math.sqrt(lastDir.x * lastDir.x + lastDir.z * lastDir.z);
        if (len > 0.0001) {
            return new Vector3d(lastDir.x / len, 0, lastDir.z / len);
        }
        return new Vector3d(0, 0, 1);
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
     * 左右手重判：名称识别可能把左右写反（有的模型部件命名相反）。
     * 按实际物理位置判断——手臂质心投影到身体"左"方向（与 {@link #legSideSign} 同一套），
     * 投影更大的在左侧。左右差太小（躺姿/贴在一起）时退回名称顺序，避免误判。
     *
     * @return [左手, 右手]
     */
    private static ServerSubLevel[] resolveArmsBySide(ServerSubLevel body, ServerSubLevel namedLeft, ServerSubLevel namedRight) {
        if (namedLeft == null || namedRight == null || namedLeft.equals(namedRight)
                || body == null || body.isRemoved()) {
            return new ServerSubLevel[]{namedLeft, namedRight};
        }
        Vector3d forward = body.logicalPose().orientation().transform(new Vector3d(0, 0, 1), new Vector3d());
        Vector3d side = new Vector3d(forward.z, 0, -forward.x);
        double projLeft = armSideProjection(body, namedLeft, side);
        double projRight = armSideProjection(body, namedRight, side);
        if (Math.abs(projLeft - projRight) < 0.1) {
            return new ServerSubLevel[]{namedLeft, namedRight};
        }
        if (projLeft >= projRight) {
            return new ServerSubLevel[]{namedLeft, namedRight};
        }
        return new ServerSubLevel[]{namedRight, namedLeft};
    }

    /** 手臂质心在身体"左"方向上的投影（正 = 实际在身体左侧）。 */
    private static double armSideProjection(ServerSubLevel body, ServerSubLevel arm, Vector3d side) {
        var bodyPose = body.logicalPose();
        Vector3d bodyPos = bodyPose.transformPosition(bodyPose.rotationPoint(), new Vector3d());
        var armPose = arm.logicalPose();
        Vector3d armPos = armPose.transformPosition(armPose.rotationPoint(), new Vector3d());
        double dx = armPos.x - bodyPos.x;
        double dz = armPos.z - bodyPos.z;
        return dx * side.x + dz * side.z;
    }

    /**
     * 向下发射一条射线（refY+1 → refY-24），返回第一个被撞到的固体表面顶面 Y（世界坐标）；没检测到方块（未命中或超出范围）返回 NaN，调用方不应添加支撑脚关节。
     * 用 level.clip 而不是 getBlockState 逐格下扫：Sable 的 clip_overwrite mixin 让 clip
     * 同时检测其他物理结构（其他布娃娃/子级结构），getBlockState 只能看到主世界地形方块。
     * 两个关键点（踩过的坑）：
     * <ul>
     *     <li>必须用 ClipContextExtension.sable$setSubLevelIgnoring 排除布娃娃自己的子级，
     *     否则射线会命中自己的身体/腿——之前"飞天"的根因。</li>
     *     <li>命中其他子级时返回的是该子级的 plotgrid 坐标（约 2000 万格外），不是世界坐标；
     *     要先用 Sable.HELPER.getContaining 找到命中的子级，
     *     再 subLevel.logicalPose().transformPosition(hit) 换算回世界坐标。</li>
     * </ul>
     */
    private double groundTopAt(double x, double z, double refY) {
        var level = body.getLevel();
        ClipContext ctx = new ClipContext(
                new Vec3(x, refY + 1.0, z),
                new Vec3(x, refY - 24.0, z),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty());
        if (ctx instanceof ClipContextExtension ext) {
            // 只排除布娃娃自己的子级：clip 会检测其他物理结构
            ext.sable$setSubLevelIgnoring(sub -> ragdoll.getSublevels().contains(sub));
        }
        BlockHitResult hit = level.clip(ctx);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return Double.NaN;
        }
        Vec3 loc = hit.getLocation();
        double y = loc.y;
        SubLevel hitSub = Sable.HELPER.getContaining(level, loc);
        if (hitSub != null) {
            // 命中其他物理结构：loc 是 plotgrid 坐标，换算回世界坐标
            y = hitSub.logicalPose().transformPosition(loc).y;
        }
        if(y - refY > 0.5) {
            if (hitSub == null)
                return Double.NaN;
            else
                y = refY;
        }
        if(y < refY)
            y = Math.max(refY - 0.5f, y);
        return y;
    }

    /**
     * 原始地面顶面 Y（世界坐标，不做任何 clamp）；扫不到返回 NaN。
     * 与 {@link #groundTopAt} 同一套 clip 探测，但保留真实高度，
     * 供 {@link #isAirborne()} 判断脚底离地距离。
     */
    private double rawGroundTopAt(double x, double z, double refY) {
        var level = body.getLevel();
        ClipContext ctx = new ClipContext(
                new Vec3(x, refY + 1.0, z),
                new Vec3(x, refY - 24.0, z),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty());
        if (ctx instanceof ClipContextExtension ext) {
            ext.sable$setSubLevelIgnoring(sub -> ragdoll.getSublevels().contains(sub));
        }
        BlockHitResult hit = level.clip(ctx);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return Double.NaN;
        }
        Vec3 loc = hit.getLocation();
        double y = loc.y;
        SubLevel hitSub = Sable.HELPER.getContaining(level, loc);
        if (hitSub != null) {
            y = hitSub.logicalPose().transformPosition(loc).y;
        }
        return y;
    }

    /**
     * 把腿的脚底中心约束到落脚点：电机目标从脚当前位置逐渐收敛，线性电机自动回正（最大力度受限）。
     * 没检测到方块（foothold.y 为 NaN，见 {@link #groundTopAt}）时不添加关节，
     * 保持原有支撑脚，避免把脚绑在悬空处。
     */
    private void lockFoot(ServerSubLevel leg, Vector3d foothold) {
        if (Double.isNaN(foothold.y)) {
            return;
        }
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
        // Y 不限制：收敛只算水平方向（高度交给重力），否则初始 Y 差会拖慢 X/Z 的收敛
        stanceTargetRel.y = 0;
        applyStanceMotors();
    }

    /**
     * 支撑脚电机：水平 X/Z 目标每刻向 0 收敛（脚底逐渐自动回正到落脚点）；
     * Y 是单向上限——只把脚往上抬到落脚点高度（上楼梯），不往下压；
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
        // Y 单向上限：脚底低于落脚点高度时往上拉（上楼梯/台阶），
        // 脚底不低于落脚点时目标=当前偏移=零力，不往下拽（高度仍靠重力回落）。
        // 电机目标是"相对世界侧帧的位移"：0 = 脚底在落脚点高度。
        double footY = footWorld(stanceLeg).y;
        double yTarget = Math.max(0, footY - stanceFramePos.y);
        try {
            stanceJoint.setMotor(ConstraintJointAxis.LINEAR_X, stanceTargetRel.x,
                    STANCE_STIFFNESS * mass, STANCE_DAMPING * mass,
                    true, STANCE_MAX_FORCE * mass);
            stanceJoint.setMotor(ConstraintJointAxis.LINEAR_Y, yTarget,
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

    /** 抓取目标：sub 为命中的物理结构（null=主世界/空抓），pos 为约束第一侧锚点坐标（世界或该子级 plotgrid），worldPos 为抓取点的世界坐标。 */
    private record GrabTarget(SubLevel sub, Vector3d pos, Vector3d worldPos) {
    }

    /**
     * 按住 Alt：进入"抬手"状态，双手一起抬起。
     * 用的是 身体↔手臂 约束 + motor target 控制方向：目标按各模型手臂自身长度算
     * （见 {@link #raisedRelOffset}），把手臂从自然下垂转到与身体垂直（水平前伸），
     * 不写死抬手高度；力只在布娃娃内部传递，不会像"往世界点拉"那样经肩关节
     * 把整个身体拽飞（踩过的坑）。
     * 抬手约束在抓取后保留：抓住物品后手臂仍会继续尝试保持抬起姿势。
     * 先不探测，松开时才决定抓什么。没有手臂时不进入该状态。
     */
    private void startReach() {
        if (grabState != GRAB_IDLE) {
            return;
        }
        boolean any = false;
        if (leftArm != null && !leftArm.isRemoved()) {
            GenericConstraintHandle joint = attachReach(leftArm);
            if (joint != null) {
                raiseJointA = joint;
                grabHandA = leftArm;
                Vector3d initial = initialRelOffset(leftArm);
                raiseTargetRelA.set(initial);
                raiseFinalRelA.set(raisedRelOffset(initial, leftArm));
                any = true;
            }
        }
        if (rightArm != null && !rightArm.isRemoved()) {
            GenericConstraintHandle joint = attachReach(rightArm);
            if (joint != null) {
                raiseJointB = joint;
                grabHandB = rightArm;
                Vector3d initial = initialRelOffset(rightArm);
                raiseTargetRelB.set(initial);
                raiseFinalRelB.set(raisedRelOffset(initial, rightArm));
                any = true;
            }
        }
        if (!any) {
            return;
        }
        grabState = GRAB_REACHING;
        applyGrabMotors();
    }

    /**
     * 抬手约束：身体 ↔ 手臂（内力约束，不会把身体拽飞）。
     * pos1 = 身体质心（身体 plot 内），pos2 = 手掌（手臂 plot 内）。
     * 方向完全由电机 target 控制（见 {@link #applyGrabMotors()}），
     * 帧跟随身体自动转动，不需要每 tick 重锚。
     */
    private GenericConstraintHandle attachReach(ServerSubLevel arm) {
        var bodyPose = body.logicalPose();
        Vector3d bodyPlot = new Vector3d(bodyPose.rotationPoint());
        var armPose = arm.logicalPose();
        Vector3d palm = partBottomWorld(arm);
        Vector3d palmPlot = armPose.transformPositionInverse(palm, new Vector3d());
        try {
            GenericConstraintHandle joint = container.physicsSystem().getPipeline().addConstraint(
                    body, arm,
                    new GenericConstraintConfiguration(
                            bodyPlot, palmPlot,
                            new Quaterniond(), new Quaterniond(),
                            Set.of()));
            if (joint != null && joint.isValid()) {
                return joint;
            }
        } catch (Exception e) {
            LOGGER.debug("创建抬手约束失败：", e);
        }
        return null;
    }

    /** 手掌相对身体质心的偏移（身体局部坐标）：抬手电机的起始目标，从"当前姿势"开始不突跳。 */
    private Vector3d initialRelOffset(ServerSubLevel arm) {
        var bodyPose = body.logicalPose();
        Vector3d bodyCenter = bodyPose.transformPosition(bodyPose.rotationPoint(), new Vector3d());
        Vector3d palm = partBottomWorld(arm);
        Vector3d rel = new Vector3d(palm).sub(bodyCenter);
        Quaterniond inv = bodyPose.orientation().conjugate(new Quaterniond());
        return inv.transform(rel, new Vector3d());
    }

    /**
     * 抬手最终目标（身体局部坐标）：从当前手掌偏移向上抬"臂长"、向前伸"臂长"。
     * 这样手臂绕肩转到与身体垂直（水平前伸），高度随模型自己手臂长度自适应，
     * 不同模型不用调参。
     */
    private Vector3d raisedRelOffset(Vector3d palmRel, ServerSubLevel arm) {
        double len = armLength(arm);
        return new Vector3d(palmRel.x, palmRel.y + len, palmRel.z + len);
    }

    /** 手臂长度（肩到手掌，方块）：手臂部件包围盒高 - 0.2，与 {@link #partBottomWorld} 的偏移一致。 */
    private double armLength(ServerSubLevel arm) {
        return Math.max(0.2, arm.getPlot().getBoundingBox().height() - 0.2);
    }

    /**
     * 松开 Alt：在"抬手"状态下从双手中间沿"手的正方向"重新探测，
     * 命中方块/物理结构就把双手一起锁到物品两侧；没命中就不抓（不抓空气），
     * 手臂放下回到空闲。抬手约束保留，抓取后手臂仍会继续尝试保持抬起的动作。
     */
    private void finishGrab() {
        if (grabState != GRAB_REACHING) {
            return;
        }
        Vector3d center = handCenter();
        if (center == null) {
            releaseGrab();
            return;
        }
        GrabTarget target = computeGrabTarget(center);
        if (target == null) {
            // 手的方向上什么都没有：不抓空气，恢复原状
            releaseGrab();
            return;
        }
        if (attachHands(target)) {
            grabState = GRAB_HOLDING;
            applyGrabMotors();
        } else {
            releaseGrab();
        }
    }

    /** 双手手掌的中点（缺失一只手时用存在的那只），没有手臂返回 null。 */
    private Vector3d handCenter() {
        Vector3d a = leftArm != null && !leftArm.isRemoved() ? partBottomWorld(leftArm) : null;
        Vector3d b = rightArm != null && !rightArm.isRemoved() ? partBottomWorld(rightArm) : null;
        if (a != null && b != null) {
            return a.add(b, new Vector3d()).mul(0.5);
        }
        return a != null ? a : b;
    }

    /** 把存在的手臂都加上抓取约束（物品两侧各一只），返回是否至少抓住了一只。
     *  抓取约束在抬手约束之外单独创建：抬手电机继续抬，抓取电机把物品拉到手上；
     *  抓到物理结构时记录结构标记，之后开角轴同步让物品跟着手臂转。 */
    private boolean attachHands(GrabTarget target) {
        boolean any = false;
        if (leftArm != null && !leftArm.isRemoved()) {
            Vector3d palm = partBottomWorld(leftArm);
            GenericConstraintHandle joint = attachHand(leftArm, palm, target);
            if (joint != null) {
                grabJointA = joint;
                grabHandA = leftArm;
                grabTargetRelA.set(palm).sub(target.worldPos());
                grabFinalRelA.set(handHoldOffset(true));
                grabIsStructureA = target.sub() != null;
                any = true;
            }
        }
        if (rightArm != null && !rightArm.isRemoved()) {
            Vector3d palm = partBottomWorld(rightArm);
            GenericConstraintHandle joint = attachHand(rightArm, palm, target);
            if (joint != null) {
                grabJointB = joint;
                grabHandB = rightArm;
                grabTargetRelB.set(palm).sub(target.worldPos());
                grabFinalRelB.set(handHoldOffset(false));
                grabIsStructureB = target.sub() != null;
                any = true;
            }
        }
        return any;
    }

    /**
     * 抓取时手掌相对抓取点的最终偏移（世界坐标）：左手往身体左侧偏、
     * 右手往右侧偏 {@value #GRAB_HAND_SPACING} 格，两只手在物品两侧，不会交叉相撞。
     */
    private Vector3d handHoldOffset(boolean isLeft) {
        Vector3d forward = body.logicalPose().orientation().transform(new Vector3d(0, 0, 1), new Vector3d());
        Vector3d side = new Vector3d(forward.z, 0, -forward.x);
        double sign = isLeft ? 1 : -1;
        return new Vector3d(side.x * sign * GRAB_HAND_SPACING, 0, side.z * sign * GRAB_HAND_SPACING);
    }

    /**
     * 为一只手创建抓取约束（自由约束 + 线性电机）：手掌被拉到抓取点（pos1），
     * 方向/转速由电机 target 控制，物品两侧各一只手的横向偏移由电机目标实现
     * （见 {@link #applyGrabMotors()}）。pos2 用手臂 plot 内的坐标（保证落在 plot 内）；
     * 命中其它子级时约束第一侧就是那个子级（结构移动时手跟着被拖住）。
     */
    private GenericConstraintHandle attachHand(ServerSubLevel hand, Vector3d palm, GrabTarget target) {
        var handPose = hand.logicalPose();
        Vector3d palmPlot = handPose.transformPositionInverse(palm, new Vector3d());
        ServerSubLevel hitServer = target.sub() instanceof ServerSubLevel ssl ? ssl : null;
        try {
            GenericConstraintHandle joint = container.physicsSystem().getPipeline().addConstraint(
                    hitServer, hand,
                    new GenericConstraintConfiguration(
                            target.pos(), palmPlot,
                            new Quaterniond(), new Quaterniond(),
                            Set.of()));
            if (joint != null && joint.isValid()) {
                return joint;
            }
        } catch (Exception e) {
            LOGGER.debug("创建抓取约束失败：", e);
        }
        return null;
    }

    /** 删除两只手的抬手约束与抓取约束（不改变状态）。 */
    private void releaseHands() {
        removeJoint(raiseJointA);
        raiseJointA = null;
        removeJoint(raiseJointB);
        raiseJointB = null;
        removeJoint(grabJointA);
        grabJointA = null;
        removeJoint(grabJointB);
        grabJointB = null;
        grabHandA = null;
        grabHandB = null;
    }

    /**
     * 手的正方向（世界坐标，归一化）：手臂部件从肩到手掌的指向（本地 -Y 轴），
     * 双手取平均。与玩家摄像机朝向无关——抬手后手往哪指就往哪抓。
     */
    private Vector3d handForward() {
        Vector3d dir = new Vector3d();
        int n = 0;
        if (leftArm != null && !leftArm.isRemoved()) {
            dir.add(leftArm.logicalPose().orientation().transform(new Vector3d(0, -1, 0), new Vector3d()));
            n++;
        }
        if (rightArm != null && !rightArm.isRemoved()) {
            dir.add(rightArm.logicalPose().orientation().transform(new Vector3d(0, -1, 0), new Vector3d()));
            n++;
        }
        if (n == 0) {
            return new Vector3d(0, 0, 1);
        }
        dir.mul(1.0 / n);
        double len = dir.length();
        if (len < 1e-4) {
            return new Vector3d(0, 0, 1);
        }
        return dir.mul(1.0 / len, new Vector3d());
    }

    /**
     * 计算抓取点：从手掌底部中心（手正中央）出发，沿"手的正方向"射线探测
     * （最远 {@value #GRAB_REACH} 格，起点往手后方缩进一点，手贴着物体时也能命中），
     * 与玩家摄像机朝向无关——抬手后手往哪指就往哪抓。
     * 命中点明显低于手掌（地面/脚下）不算；没命中返回 null（不抓空气）。
     * 每次计算都会更新渲染用的射线（{@link #debugRayOrigin} / {@link #debugRayEnd}），
     * 客户端据此画出抓取位置。
     * 命中其它物理结构时 pos 是 plotgrid 坐标、sub 是命中的子级；
     * 命中主世界方块时 pos 是世界坐标、sub 为 null。
     */
    private GrabTarget computeGrabTarget(Vector3d palmCenter) {
        Vector3d dir = handForward();
        Vector3d origin = new Vector3d(
                palmCenter.x - dir.x * GRAB_RAY_INSET,
                palmCenter.y - dir.y * GRAB_RAY_INSET,
                palmCenter.z - dir.z * GRAB_RAY_INSET);
        Vector3d end = new Vector3d(
                origin.x + dir.x * (GRAB_REACH + GRAB_RAY_INSET),
                origin.y + dir.y * (GRAB_REACH + GRAB_RAY_INSET),
                origin.z + dir.z * (GRAB_REACH + GRAB_RAY_INSET));
        ClipContext ctx = new ClipContext(
                new Vec3(origin.x, origin.y, origin.z),
                new Vec3(end.x, end.y, end.z),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty());
        if (ctx instanceof ClipContextExtension ext) {
            // 只排除布娃娃自己的子级：其它布娃娃/物理结构都可以被抓
            ext.sable$setSubLevelIgnoring(sub -> ragdoll.getSublevels().contains(sub));
        }
        BlockHitResult hit = body.getLevel().clip(ctx);
        if (hit.getType() != HitResult.Type.BLOCK) {
            debugRayOrigin.set(origin);
            debugRayEnd.set(end);
            debugRayHit = false;
            return null;
        }
        Vec3 loc = hit.getLocation();
        Vector3d plotPos = new Vector3d(loc.x, loc.y, loc.z);
        SubLevel hitSub = Sable.HELPER.getContaining(body.getLevel(), loc);
        Vector3d worldPos = hitSub != null
                ? hitSub.logicalPose().transformPosition(plotPos, new Vector3d())
                : plotPos;
        debugRayOrigin.set(origin);
        debugRayEnd.set(worldPos);
        debugRayHit = true;
        // 明显低于手掌的命中（抬手时很容易戳到脚边/地面）不算抓取目标
        if (worldPos.y < palmCenter.y - GRAB_MIN_HEIGHT_DROP) {
            return null;
        }
        if (hitSub != null) {
            return new GrabTarget(hitSub, plotPos, worldPos);
        }
        return new GrabTarget(null, plotPos, plotPos);
    }

    /**
     * 抓取电机（用 motor 控制方向，不靠硬锁）：
     * <ul>
     *     <li>抬手电机（身体↔手臂）：始终运行。目标 = 身体局部坐标下的抬手向量
     *     （每只手臂按自身长度算，转到与身体垂直，见 {@link #raisedRelOffset}），
     *     慢速收敛（{@value #GRAB_TARGET_STEP} 格/刻）。力只在布娃娃内部传递，
     *     不会把身体拽飞——上一版把手臂往世界点拉，外力经肩关节传到身体，
     *     一按 Alt 整个人就往上飞。</li>
     *     <li>抓取电机（世界/结构↔手臂）：松开 Alt 后才运行，快速收敛
     *     （{@value #GRAB_HOLD_STEP} 格/刻）到物品两侧（见 {@link #handHoldOffset}）；
     *     命中物理结构时额外开角轴同步电机（目标 0），让物品跟着手臂一起转。</li>
     * </ul>
     */
    private void applyGrabMotors() {
        // 抬手电机：按住前伸、抓取后保持
        applyGrabMotor(raiseJointA, grabHandA, raiseTargetRelA, raiseFinalRelA, GRAB_TARGET_STEP, false);
        applyGrabMotor(raiseJointB, grabHandB, raiseTargetRelB, raiseFinalRelB, GRAB_TARGET_STEP, false);
        // 抓取电机：松开 Alt 后才创建；结构抓取带角轴同步
        if (grabState == GRAB_HOLDING) {
            applyGrabMotor(grabJointA, grabHandA, grabTargetRelA, grabFinalRelA, GRAB_HOLD_STEP, grabIsStructureA);
            applyGrabMotor(grabJointB, grabHandB, grabTargetRelB, grabFinalRelB, GRAB_HOLD_STEP, grabIsStructureB);
        }
    }

    /** 单个手臂的电机：targetRel 每刻向 finalTarget 收敛（最多 step 格），
     *  再把当前目标设给电机。参数是约束帧坐标（身体↔手臂 = 身体局部轴；
     *  世界/结构↔手臂 = 世界/结构局部轴），带最大力度上限防止拽飞身体。
     *  抬手慢收敛、抓取快收敛，步长由调用方决定。
     *  syncRotation 为 true（抓到物理结构）时额外开角轴电机（目标 0），
     *  让物品与手臂的相对转角归零，物品跟着手臂一起转。 */
    private void applyGrabMotor(GenericConstraintHandle joint, ServerSubLevel hand, Vector3d targetRel, Vector3d finalTarget, double step, boolean syncRotation) {
        if (joint == null || !joint.isValid() || hand == null) {
            return;
        }
        Vector3d diff = new Vector3d(finalTarget).sub(targetRel);
        double dist = diff.length();
        if (dist <= step) {
            targetRel.set(finalTarget);
        } else {
            targetRel.fma(step / dist, diff);
        }
        double mass = massOf(hand);
        try {
            joint.setMotor(ConstraintJointAxis.LINEAR_X, targetRel.x,
                    GRAB_STIFFNESS * mass, GRAB_DAMPING * mass, true, GRAB_MAX_FORCE * mass);
            joint.setMotor(ConstraintJointAxis.LINEAR_Y, targetRel.y,
                    GRAB_STIFFNESS * mass, GRAB_DAMPING * mass, true, GRAB_MAX_FORCE * mass);
            joint.setMotor(ConstraintJointAxis.LINEAR_Z, targetRel.z,
                    GRAB_STIFFNESS * mass, GRAB_DAMPING * mass, true, GRAB_MAX_FORCE * mass);
            if (syncRotation) {
                joint.setMotor(ConstraintJointAxis.ANGULAR_X, 0,
                        GRAB_ANGULAR_STIFFNESS * mass, GRAB_ANGULAR_DAMPING * mass, true, GRAB_ANGULAR_MAX_TORQUE * mass);
                joint.setMotor(ConstraintJointAxis.ANGULAR_Y, 0,
                        GRAB_ANGULAR_STIFFNESS * mass, GRAB_ANGULAR_DAMPING * mass, true, GRAB_ANGULAR_MAX_TORQUE * mass);
                joint.setMotor(ConstraintJointAxis.ANGULAR_Z, 0,
                        GRAB_ANGULAR_STIFFNESS * mass, GRAB_ANGULAR_DAMPING * mass, true, GRAB_ANGULAR_MAX_TORQUE * mass);
            }
        } catch (Exception e) {
            LOGGER.debug("设置抓取电机失败：", e);
        }
    }

    /** 取消抓取/恢复：删除双手约束，手臂自然垂落。 */
    private void releaseGrab() {
        releaseHands();
        grabState = GRAB_IDLE;
    }

    /** 把抓取射线发给客户端（渲染用）。visible=false 表示清除射线（离开瞄准状态）。 */
    private void sendGrabRay(boolean visible) {
        if (player == null || player.connection == null) {
            return;
        }
        try {
            player.connection.send(new ClientboundCustomPayloadPacket(
                    new ClientboundRagdollGrabRayPacket(
                            new Vec3(debugRayOrigin.x, debugRayOrigin.y, debugRayOrigin.z),
                            new Vec3(debugRayEnd.x, debugRayEnd.y, debugRayEnd.z),
                            debugRayHit, visible)));
        } catch (Exception ignored) {
        }
        debugRayVisible = visible;
    }

    /** 部件底端中心的粗略世界位置：沿部件本地 -Y 方向偏移半格（腿=脚底，手臂=手掌）。
     *  必须先本地偏移再变换——手臂抬起后本地 -Y 不再是世界下方，
     *  直接在世界上往下减会把"手掌"算到肚子位置（踩过的坑）。 */
    private Vector3d partBottomWorld(ServerSubLevel part) {
        var pose = part.logicalPose();
        var h = part.getPlot().getBoundingBox().height() / 2d - 0.1;
        var bottomLocal = new Vector3d(pose.rotationPoint()).sub(0, h + 0.1f, 0, new Vector3d());
        return pose.transformPosition(bottomLocal, new Vector3d());
    }

    /** 腿的脚底中心世界位置：部件底端。 */
    private Vector3d footWorld(ServerSubLevel leg) {
        return partBottomWorld(leg);
    }

    private static double massOf(ServerSubLevel subLevel) {
        var tracker = subLevel.getMassTracker();
        if (tracker != null && tracker.getMass() > 0.001) {
            return tracker.getMass();
        }
        return 1.0;
    }

    private static void removeJoint(PhysicsConstraintHandle joint) {
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