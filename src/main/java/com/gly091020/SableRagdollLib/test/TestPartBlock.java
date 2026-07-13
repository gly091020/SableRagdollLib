package com.gly091020.SableRagdollLib.test;

import com.gly091020.SableRagdollLib.block.AbstractPartBlock;
import com.gly091020.SableRagdollLib.block.AbstractPartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class TestPartBlock extends AbstractPartBlock {
    protected TestPartBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Function<Properties, AbstractPartBlock> createBlock() {
        return TestPartBlock::new;
    }

    @Override
    public @NotNull AbstractPartBlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new TestPartBlockEntity(blockPos, blockState);
    }
}
