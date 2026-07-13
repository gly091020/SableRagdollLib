package com.gly091020.SableRagdollLib.test;

import com.gly091020.SableRagdollLib.SableRagdollLib;
import com.gly091020.SableRagdollLib.api.RagdollTypeRegistry;
import com.gly091020.SableRagdollLib.block.AbstractPartBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class TestMain {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, SableRagdollLib.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SableRagdollLib.MODID);

    public static final DeferredHolder<Block, TestPartBlock> TEST_PART_BLOCK = BLOCKS.register("test_part",
            r -> new TestPartBlock(AbstractPartBlock.BASE_PROPERTIES));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TestPartBlockEntity>> TEST_PART_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("test_part",
            r -> BlockEntityType.Builder.of(TestPartBlockEntity::new, TEST_PART_BLOCK.get()).build(null));

    public static void init(IEventBus bus){
        BLOCKS.register(bus);
        BLOCK_ENTITY_TYPES.register(bus);
        RagdollTypeRegistry.registry(ResourceLocation.fromNamespaceAndPath(SableRagdollLib.MODID, "test"), TEST_PART_BLOCK::get, TEST_PART_BLOCK_ENTITY::get);
    }
}
