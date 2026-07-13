package com.gly091020.SableRagdollLib.test;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public class TestMainClient {
    public static void init(IEventBus bus){
        bus.register(EventHandler.class);
    }

    public static class EventHandler{
        @SubscribeEvent
        public static void registryRenderer(EntityRenderersEvent.RegisterRenderers event){
            event.registerBlockEntityRenderer(TestMain.TEST_PART_BLOCK_ENTITY.get(), TestPartBlockRenderer::new);
        }
    }
}
