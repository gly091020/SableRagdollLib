package com.gly091020.SableRagdollLib;

import com.gly091020.SableRagdollLib.client.renderer.PartSeatRenderer;
import com.gly091020.SableRagdollLib.client.RagdollDragClient;
import com.gly091020.SableRagdollLib.command.SableRagdollLibClientCommand;
import com.gly091020.SableRagdollLib.editor.EditorOpener;
import com.gly091020.SableRagdollLib.entity.PartSeat;
import com.gly091020.SableRagdollLib.test.TestMainClient;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.lwjgl.glfw.GLFW;

import static com.gly091020.SableRagdollLib.SableRagdollLib.PART_SEAT;

@Mod(dist = Dist.CLIENT, value = SableRagdollLib.MODID)
public class SableRagdollLibClient {
    public static final KeyMapping OPEN_EDITOR = new KeyMapping(
            "key.sableragdolllib.open_editor",
            GLFW.GLFW_KEY_UNKNOWN,
            "key.category.sableragdolllib"
    );

    public SableRagdollLibClient(IEventBus bus, ModContainer mc){
        if(!FMLEnvironment.production)
            TestMainClient.init(bus);
        bus.addListener(EventHandler::onClientInit);

        mc.registerExtensionPoint(IConfigScreenFactory.class,
                (mc1, p) -> AutoConfig.getConfigScreen(SableRagdollLibConfig.class, p).get());
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
            RagdollDragClient.tick();
        }

        @SubscribeEvent
        public static void onRenderEntity(RenderLivingEvent.Pre<?, ?> event){
            if(event.getEntity().getVehicle() instanceof PartSeat)event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onRenderGui(RenderGuiEvent.Post event){
            // 拖动布娃娃时在 HUD 上方显示提示
            if(!RagdollDragClient.isDragging())return;
            var mc = Minecraft.getInstance();
            var component = Component.translatable("text.sableragdolllib.dragging");
            int x = (mc.getWindow().getGuiScaledWidth() - mc.font.width(component)) / 2;
            int y = mc.getWindow().getGuiScaledHeight() - 70;
            event.getGuiGraphics().drawString(mc.font, component, x, y, 0xFFFFFF, true);
        }

        @SubscribeEvent
        public static void onInteractBlock(PlayerInteractEvent.LeftClickBlock event){
            if(RagdollDragClient.isDragging())event.setCanceled(true);
        }
    }
}
