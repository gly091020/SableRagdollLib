package com.gly091020.SableRagdollLib.compat.player_ragdoll;

import com.gly091020.SableRagdollLib.api.control.RagdollControlManager;
import com.gly091020.SableRagdollLib.api.control.RagdollControlSession;
import dev.leo.sableplayerragdoll.api.RagdollAPI;
import dev.leo.sableplayerragdoll.block.entity.RagdollPartBlockEntity;
import dev.leo.sableplayerragdoll.physics.RagdollAssemblyHelper;
import dev.leo.sableplayerragdoll.physics.RagdollSessionManager;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PlayerRagdollUtil {
    public static RagdollControlSession startControl(ServerPlayer player){
        ServerSubLevel body = RagdollSessionManager.activeRagdollForPlayer(player.serverLevel(), player.getUUID());
        if(body == null)return null;
        var container = ServerSubLevelContainer.getContainer(body.getLevel());
        var subs = RagdollAssemblyHelper.linkedPartsAsMap(body.getUniqueId());

        var head = getSubLevel(container, subs.get(RagdollPartBlockEntity.BodyPart.HEAD));
        var leftArm = getSubLevel(container, subs.get(RagdollPartBlockEntity.BodyPart.LEFT_ARM));
        var rightArm = getSubLevel(container, subs.get(RagdollPartBlockEntity.BodyPart.RIGHT_ARM));
        var leftLeg = getSubLevel(container, subs.get(RagdollPartBlockEntity.BodyPart.LEFT_LEG));
        var rightLeg = getSubLevel(container, subs.get(RagdollPartBlockEntity.BodyPart.RIGHT_LEG));

        if(head == null || leftArm == null || leftLeg == null || rightArm == null || rightLeg == null)return null;

        var session = RagdollControlSession.create(player, body, leftLeg, rightLeg, leftArm, rightArm, head);
        RagdollControlManager.start(player, session);
        return session;
    }

    public static boolean isRagdoll(ServerPlayer player){
        return RagdollAPI.isRagdolled(player);
    }

    public static ServerSubLevel getSubLevel(ServerSubLevelContainer container, @Nullable UUID uuid){
        if(uuid == null)return null;
        else return (ServerSubLevel) container.getSubLevel(uuid);
    }
}
