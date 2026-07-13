package com.gly091020.SableRagdollLib.test;

import com.gly091020.SableRagdollLib.block.AbstractPartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class TestPartBlockEntity extends AbstractPartBlockEntity {
    public TestPartBlockEntity(BlockPos pos, BlockState state) {
        super(TestMain.TEST_PART_BLOCK_ENTITY.get(), pos, state);
    }
}
