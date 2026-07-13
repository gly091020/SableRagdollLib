package com.gly091020.SableRagdollLib;

import com.gly091020.SableRagdollLib.command.SableRagdollLibClientCommand;
import com.gly091020.SableRagdollLib.test.TestMainClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@Mod(dist = Dist.CLIENT, value = SableRagdollLib.MODID)
public class SableRagdollLibClient {
    public SableRagdollLibClient(IEventBus bus){
        if(!FMLEnvironment.production)
            TestMainClient.init(bus);
    }

    @EventBusSubscriber(modid = SableRagdollLib.MODID, value = Dist.CLIENT)
    public static class EventHandler{
        @SubscribeEvent
        public static void registerClientCommands(RegisterClientCommandsEvent event) {
            SableRagdollLibClientCommand.registry(event.getDispatcher());
        }
    }
}
