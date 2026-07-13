package com.gly091020.SableRagdollLib;

import com.gly091020.SableRagdollLib.api.RagdollManager;
import com.gly091020.SableRagdollLib.command.SableRagdollLibCommand;
import com.gly091020.SableRagdollLib.common.PartColliderBoxManager;
import com.gly091020.SableRagdollLib.common.RagdollReloadListener;
import com.gly091020.SableRagdollLib.test.TestMain;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(SableRagdollLib.MODID)
public class SableRagdollLib {
    public static final String MODID = "sableragdolllib";
    public static SableRagdollLibConfig config;

    public SableRagdollLib(IEventBus bus){
        config = AutoConfig.register(SableRagdollLibConfig.class, Toml4jConfigSerializer::new).getConfig();
        if(!FMLEnvironment.production)
            TestMain.init(bus);
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
        }

        @SubscribeEvent
        public static void onServerTick(ServerTickEvent.Post event){
            RagdollManager.tick();
        }
    }
}
