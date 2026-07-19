package com.gly091020.SableRagdollLib;

import com.gly091020.SableRagdollLib.client.renderer.PartSeatRenderer;
import com.gly091020.SableRagdollLib.command.SableRagdollLibClientCommand;
import com.gly091020.SableRagdollLib.editor.EditorOpener;
import com.gly091020.SableRagdollLib.test.TestMainClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

import static com.gly091020.SableRagdollLib.SableRagdollLib.PART_SEAT;

@Mod(dist = Dist.CLIENT, value = SableRagdollLib.MODID)
public class SableRagdollLibClient {
    public static final KeyMapping OPEN_EDITOR = new KeyMapping(
            "key.sableragdolllib.open_editor",
            GLFW.GLFW_KEY_UNKNOWN,
            "key.category.sableragdolllib"
    );

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

        @SubscribeEvent
        public static void onRegistryKey(RegisterKeyMappingsEvent event){
            event.register(OPEN_EDITOR);
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Pre event){
            while (OPEN_EDITOR.consumeClick()){
                if(SableRagdollLib.hasLDLib()) EditorOpener.open();
            }
        }
    }
}
