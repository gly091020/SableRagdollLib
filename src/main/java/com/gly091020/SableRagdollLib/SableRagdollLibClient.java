package com.gly091020.SableRagdollLib;

import com.gly091020.SableRagdollLib.client.renderer.PartSeatRenderer;
import com.gly091020.SableRagdollLib.command.SableRagdollLibClientCommand;
import com.gly091020.SableRagdollLib.test.TestMainClient;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import static com.gly091020.SableRagdollLib.SableRagdollLib.PART_SEAT;

@Mod(dist = Dist.CLIENT, value = SableRagdollLib.MODID)
public class SableRagdollLibClient {
    public SableRagdollLibClient(IEventBus bus){
        if(!FMLEnvironment.production)
            TestMainClient.init(bus);
        bus.addListener(EventHandler::onClientInit);
    }

    @EventBusSubscriber(modid = SableRagdollLib.MODID, value = Dist.CLIENT)
    public static class EventHandler{
        @SubscribeEvent
        public static void registerClientCommands(RegisterClientCommandsEvent event) {
            SableRagdollLibClientCommand.registry(event.getDispatcher());
        }

        public static void onClientInit(FMLClientSetupEvent event){
            EntityRenderers.register(
                    PART_SEAT.get(),
                    PartSeatRenderer::new
            );
        }
    }
}
