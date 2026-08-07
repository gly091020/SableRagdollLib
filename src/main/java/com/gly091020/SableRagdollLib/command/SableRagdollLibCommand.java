package com.gly091020.SableRagdollLib.command;

import com.gly091020.SableRagdollLib.api.Ragdoll;
import com.gly091020.SableRagdollLib.api.RagdollHelper;
import com.gly091020.SableRagdollLib.api.RagdollManager;
import com.gly091020.SableRagdollLib.api.RagdollTypeRegistry;
import com.gly091020.SableRagdollLib.api.control.RagdollControlManager;
import com.gly091020.SableRagdollLib.api.control.RagdollControlSession;
import com.gly091020.SableRagdollLib.api.control.RagdollPartRecognizerRegistry;
import com.gly091020.SableRagdollLib.common.DefFileLoader;
import com.gly091020.SableRagdollLib.common.RagdollReloadListener;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.CompletableFuture;

public class SableRagdollLibCommand {
    public static final String COMMAND = "sable_ragdoll_lib";
    /** 瞄准半径：视线方向多远内可以选中的布娃娃 */
    private static final double LOOK_RANGE = 20;
    /** 瞄准锥半角（度） */
    private static final double LOOK_CONE = 20;
    /** 无瞄准目标时的最近兜底半径 */
    private static final double NEAR_RANGE = 16;

    public static void registry(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal(COMMAND);

        root.requires(source -> source.hasPermission(2));
        root.then(Commands.literal("reload").executes(SableRagdollLibCommand::reload));
        root.then(Commands.literal("roles")
                .then(Commands.argument("id", ResourceLocationArgument.id())
                        .suggests(RagdollSuggestionProvider.INSTANCE)
                        .executes(SableRagdollLibCommand::roles)));
        root.then(Commands.literal("create")
                .then(Commands.argument("id", ResourceLocationArgument.id())
                        .suggests(RagdollSuggestionProvider.INSTANCE)
                        .executes(SableRagdollLibCommand::create)));
        root.then(Commands.literal("control")
                .executes(SableRagdollLibCommand::controlStart)
                .then(Commands.literal("stop").executes(SableRagdollLibCommand::controlStop)));

        dispatcher.register(root);
    }

    public static int create(CommandContext<CommandSourceStack> context){
        var id = ResourceLocationArgument.getId(context, "id");
        var defFile = DefFileLoader.getDefFile(id);
        if(defFile == null){
            context.getSource().sendFailure(Component.translatable("command.sableragdolllib.create.no_id"));
            return 0;
        }
        var type = RagdollTypeRegistry.getRagdollType(defFile.type());
        if(type == null){
            context.getSource().sendFailure(Component.translatable("command.sableragdolllib.create.no_id"));
            return 0;
        }

        try{
            RagdollHelper.createRagdoll(context.getSource().getLevel(), context.getSource().getPosition(), id);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.translatable("command.sableragdolllib.create.error").append(e.getLocalizedMessage()));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.translatable("command.sableragdolllib.create.success", id.toString()), false);
        return 1;
    }

    public static int roles(CommandContext<CommandSourceStack> context){
        var id = ResourceLocationArgument.getId(context, "id");
        var roles = RagdollPartRecognizerRegistry.recognize(id);
        context.getSource().sendSuccess(() -> Component.literal("roles " + id + ": " + roles), false);
        return 1;
    }

    public static int reload(CommandContext<CommandSourceStack> context){
        var resourceManager = context.getSource().getLevel().getServer().getResourceManager();
        RagdollReloadListener.reload(resourceManager);
        context.getSource().sendSuccess(() -> Component.translatable("command.sableragdolllib.reload"), false);
        return 1;
    }

    /** 开始控制：优先选视线锥内最近的布娃娃，没有则退化为最近布娃娃。 */
    public static int controlStart(CommandContext<CommandSourceStack> context){
        var source = context.getSource();
        if(!(source.getEntity() instanceof ServerPlayer player)){
            source.sendFailure(Component.translatable("command.sableragdolllib.control.need_player"));
            return 0;
        }
        Ragdoll ragdoll = findTarget(player);
        if(ragdoll == null){
            source.sendFailure(Component.translatable("command.sableragdolllib.control.no_ragdoll"));
            return 0;
        }
        RagdollControlSession session = RagdollControlManager.start(player, ragdoll);
        if(session == null){
            source.sendFailure(Component.translatable("command.sableragdolllib.control.start_failed"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.sableragdolllib.control.started", ragdoll.getUuid().toString()), false);
        return 1;
    }

    /** 结束当前玩家正在控制的布娃娃。 */
    public static int controlStop(CommandContext<CommandSourceStack> context){
        var source = context.getSource();
        if(!(source.getEntity() instanceof ServerPlayer player)){
            source.sendFailure(Component.translatable("command.sableragdolllib.control.need_player"));
            return 0;
        }
        RagdollControlSession session = RagdollControlManager.get(player);
        if(session == null){
            source.sendFailure(Component.translatable("command.sableragdolllib.control.not_controlling"));
            return 0;
        }
        RagdollControlManager.stop(player);
        source.sendSuccess(() -> Component.translatable("command.sableragdolllib.control.stopped"), false);
        return 1;
    }

    /** 先按视线锥选最近的布娃娃，没选到则退化为最近布娃娃。 */
    private static Ragdoll findTarget(ServerPlayer player){
        var level = player.serverLevel();
        var eye = player.getEyePosition();
        var look = player.getLookAngle();
        double coneTan = Math.tan(Math.toRadians(LOOK_CONE));

        Ragdoll best = null;
        double bestScore = Double.MAX_VALUE;
        Ragdoll nearest = null;
        double nearestDist = NEAR_RANGE;

        for(Ragdoll ragdoll : RagdollManager.getAll()){
            if(!ragdoll.isAlive() || !ragdoll.isLoad())continue;
            if(ragdoll.getSublevels().isEmpty() || ragdoll.getSublevels().getFirst().getLevel() != level)continue;
            var center = ragdoll.getCenterPosition();
            var toCenter = new Vec3(center.x - eye.x, center.y - eye.y, center.z - eye.z);
            double dist = toCenter.length();
            if(dist > LOOK_RANGE && dist > nearestDist)continue;

            // 最近兜底
            if(dist < nearestDist){
                nearestDist = dist;
                nearest = ragdoll;
            }

            // 视线锥：沿视线投影距离 t，垂直距离不超过 t * tan(半角)
            double t = toCenter.dot(look);
            if(t <= 0 || t > LOOK_RANGE)continue;
            double perp = toCenter.distanceTo(look.scale(t));
            if(perp > t * coneTan)continue;
            if(t < bestScore){
                bestScore = t;
                best = ragdoll;
            }
        }
        return best != null ? best : nearest;
    }

    public static class RagdollSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
        public static final RagdollSuggestionProvider INSTANCE = new RagdollSuggestionProvider();
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
            DefFileLoader.getAllKeys().forEach(r -> builder.suggest(r.toString()));
            return builder.buildFuture();
        }
    }
}