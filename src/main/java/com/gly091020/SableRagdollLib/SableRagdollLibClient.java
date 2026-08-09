package com.gly091020.SableRagdollLib;

import com.gly091020.SableRagdollLib.client.RagdollControlClient;
import com.gly091020.SableRagdollLib.client.RagdollGrabRayRenderer;
import com.gly091020.SableRagdollLib.client.RagdollDragClient;
import com.gly091020.SableRagdollLib.client.renderer.PartSeatRenderer;
import com.gly091020.SableRagdollLib.command.SableRagdollLibClientCommand;
import com.gly091020.SableRagdollLib.editor.EditorOpener;
import com.gly091020.SableRagdollLib.entity.PartSeat;
import com.gly091020.SableRagdollLib.test.TestMainClient;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
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
import net.neoforged.neoforge.client.event.*;
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
    /** 木偶模式抓取键：按下后让布娃娃的手向前伸并抓住视线方向上的方块/物理结构，再按一次取消 */
    public static final KeyMapping GRAB = new KeyMapping(
            "key.sableragdolllib.grab",
            GLFW.GLFW_KEY_LEFT_ALT,
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
            event.register(GRAB);
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Pre event){
            while (OPEN_EDITOR.consumeClick()){
                if(SableRagdollLib.hasLDLib()) EditorOpener.open();
            }
            RagdollDragClient.tick();
            RagdollControlClient.tick();
        }

        @SubscribeEvent
        public static void onMovementInput(MovementInputUpdateEvent event){
            RagdollControlClient.handleMovementInput(event);
        }

        @SubscribeEvent
        public static void onRenderLevel(RenderLevelStageEvent event){
            if(event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)return;
            RagdollGrabRayRenderer.render(event.getPoseStack(), Minecraft.getInstance().renderBuffers().bufferSource(), event.getCamera().getPosition());
        }

        @SubscribeEvent
        public static void onRenderEntity(RenderLivingEvent.Pre<?, ?> event){
            if(event.getEntity().getVehicle() instanceof PartSeat)event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onRenderGui(RenderGuiEvent.Post event){
            if(Minecraft.getInstance().options.hideGui)return;
            var mc = Minecraft.getInstance();
            // 木偶控制时显示提示
            if(RagdollControlClient.isControlling()){
                var controlText = Component.translatable("text.sableragdolllib.controlling");
                int cx = (mc.getWindow().getGuiScaledWidth() - mc.font.width(controlText)) / 2;
                int cy = mc.getWindow().getGuiScaledHeight() - 90;
                event.getGuiGraphics().drawString(mc.font, controlText, cx, cy, 0xFFFFFF, true);
            }
            // 拖动布娃娃时在 HUD 上方显示提示
            if(!RagdollDragClient.isDragging())return;
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
