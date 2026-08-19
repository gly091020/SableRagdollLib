package com.gly091020.SableRagdollLib;

import com.gly091020.SableRagdollLib.api.RagdollDragManager;
import com.gly091020.SableRagdollLib.api.control.PartRole;
import com.gly091020.SableRagdollLib.api.control.RagdollControlManager;
import com.gly091020.SableRagdollLib.api.control.RagdollPartRecognizerRegistry;
import com.gly091020.SableRagdollLib.api.RagdollManager;
import com.gly091020.SableRagdollLib.api.ScheduleManager;
import com.gly091020.SableRagdollLib.block.AbstractPartBlock;
import com.gly091020.SableRagdollLib.command.SableRagdollLibCommand;
import com.gly091020.SableRagdollLib.common.PartColliderBoxManager;
import com.gly091020.SableRagdollLib.common.RagdollReloadListener;
import com.gly091020.SableRagdollLib.common.ServerGetter;
import com.gly091020.SableRagdollLib.entity.PartSeat;
import com.gly091020.SableRagdollLib.network.*;
import com.gly091020.SableRagdollLib.test.TestMain;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod(SableRagdollLib.MODID)
public class SableRagdollLib {
    public static final String MODID = "sableragdolllib";
    public static SableRagdollLibConfig config;

    public static DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static DeferredRegister<EntityDataSerializer<?>> ENTITY_DATA_SERIALIZERS = DeferredRegister.create(NeoForgeRegistries.ENTITY_DATA_SERIALIZERS, MODID);
    public static DeferredHolder<EntityType<?>, EntityType<PartSeat>> PART_SEAT = ENTITY_TYPES.register("part_seat", r ->
            EntityType.Builder.of(PartSeat::new, MobCategory.MISC)
                    .sized(0.0F, 0.0F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("part_seat")
    );
    public static DeferredHolder<EntityDataSerializer<?>, EntityDataSerializer<Map<PartRole, UUID>>> PART_ROLES = ENTITY_DATA_SERIALIZERS.register("part_roles", r ->
            EntityDataSerializer.forValueType(
                    ByteBufCodecs.map(HashMap::new, PartRole.STREAM_CODEC, UUIDUtil.STREAM_CODEC)
    ));

    public SableRagdollLib(IEventBus bus){
        config = AutoConfig.register(SableRagdollLibConfig.class, Toml4jConfigSerializer::new).getConfig();
        if(!FMLEnvironment.production)
            TestMain.init(bus);
        ENTITY_TYPES.register(bus);
        ENTITY_DATA_SERIALIZERS.register(bus);
        bus.addListener(Network::onRegisterPayloadHandlers);
    }

    public static boolean hasLDLib(){
        return ModList.get().isLoaded("ldlib2");
    }

    public static class Network {
        public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
            var registrar = event.registrar(MODID).versioned("1");
            registrar.playToServer(
                ServerboundDragRagdollPacket.TYPE,
                ServerboundDragRagdollPacket.STREAM_CODEC,
                ServerboundDragRagdollPacket::handle
            );
            registrar.playToClient(
                    ClientboundRagdollControlPacket.TYPE,
                    ClientboundRagdollControlPacket.STREAM_CODEC,
                    ClientboundRagdollControlPacket::handle
            );
            registrar.playToServer(
                    ServerboundRagdollControlInputPacket.TYPE,
                    ServerboundRagdollControlInputPacket.STREAM_CODEC,
                    ServerboundRagdollControlInputPacket::handle
            );
            registrar.playToServer(
                    ServerboundSwitchControlModePacket.TYPE,
                    ServerboundSwitchControlModePacket.STREAM_CODEC,
                    ServerboundSwitchControlModePacket::handle
            );
            registrar.playToClient(
                    ClientboundRagdollGrabRayPacket.TYPE,
                    ClientboundRagdollGrabRayPacket.STREAM_CODEC,
                    ClientboundRagdollGrabRayPacket::handle
            );
        }
    }

    @EventBusSubscriber(modid = MODID)
    public static class EventHandler{
        @SubscribeEvent
        public static void registryReloadListener(AddReloadListenerEvent event){
            event.addListener(new RagdollReloadListener());
        }

        @SubscribeEvent
        public static void registryCommand(RegisterCommandsEvent event){
            SableRagdollLibCommand.registry(event.getDispatcher());
        }

        @SubscribeEvent
        public static void onServerStop(ServerStoppingEvent event){
            PartColliderBoxManager.reset();
            RagdollManager.reset();
            RagdollPartRecognizerRegistry.clear();
            RagdollDragManager.reset();
            RagdollControlManager.reset();
            ServerGetter.server = null;
        }

        @SubscribeEvent
        public static void onServerStart(ServerStartingEvent event){
            ServerGetter.server = event.getServer();
        }

        @SubscribeEvent
        public static void onServerTick(ServerTickEvent.Post event){
            RagdollManager.tick();
            RagdollDragManager.tick();
            RagdollControlManager.tick();
            ScheduleManager.tick(event.getServer());
        }

        @SubscribeEvent
        public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event){
            // 部件方块交给拖拽逻辑处理，阻止原版方块交互
            if(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof AbstractPartBlock)
                event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event){
            if(event.getEntity() instanceof Player player){
                RagdollDragManager.endDrag(player.level(), player);
                RagdollControlManager.stop(player);
            }
        }

        @SubscribeEvent
        public static void onInteractEntity(PlayerInteractEvent.EntityInteract event){
            if(event.getTarget().getVehicle() instanceof PartSeat)event.setCancellationResult(InteractionResult.PASS);
        }

        @SubscribeEvent
        public static void onAttackEntity(AttackEntityEvent event){
            if(event.getTarget().getVehicle() instanceof PartSeat)event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onEntityHurt(LivingIncomingDamageEvent event){
            if(event.getEntity().getVehicle() instanceof PartSeat){
                // 控制布娃娃的玩家允许受伤（用于受伤眩晕），其余骑乘者按原逻辑免疫
                boolean controlling = event.getEntity() instanceof Player player && RagdollControlManager.get(player) != null;
                if((!SableRagdollLib.config.enableRagdollHurt && !controlling) || event.getSource().is(DamageTypes.IN_WALL))
                    event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onPlayerHurt(LivingDamageEvent.Pre event){
            if(!SableRagdollLib.config.stunOnHurt)return;
            if(event.getEntity() instanceof ServerPlayer player && event.getSource().is(DamageTypes.FLY_INTO_WALL) && event.getNewDamage() > 0.1){
                var session = RagdollControlManager.get(player);
                if(session != null)
                    session.stun(60);
            }
        }

        @SubscribeEvent
        public static void onDrop(LivingFallEvent event){
            if(event.getEntity().getVehicle() instanceof PartSeat)event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onEntitySound(PlayLevelSoundEvent.AtEntity event) {
            if(event.getSound() == null ||
                    !(event.getSound().is(SoundEvents.GENERIC_BIG_FALL.getLocation()) ||
                    event.getSound().is(SoundEvents.GENERIC_SMALL_FALL.getLocation()) ||
                    event.getSound().is(SoundEvents.PLAYER_SMALL_FALL.getLocation()) ||
                    event.getSound().is(SoundEvents.PLAYER_BIG_FALL.getLocation())))
                return;
            Entity entity = event.getEntity();

            if (entity instanceof Player player
                    && player.getVehicle() instanceof PartSeat) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onEntityDie(LivingDeathEvent event){
            if(event.getEntity().getVehicle() instanceof PartSeat && event.getEntity().deathTime > 0)
                event.getEntity().stopRiding();
        }
    }
}
