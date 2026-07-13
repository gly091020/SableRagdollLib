package com.gly091020.SableRagdollLib.block;

import com.gly091020.SableRagdollLib.api.RagdollTypeRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public abstract class AbstractPartBlock extends BaseEntityBlock {
    public static final Properties BASE_PROPERTIES = Properties.ofFullCopy(Blocks.WHITE_WOOL)
            .noLootTable()
            .sound(SoundType.WOOL)
            .isValidSpawn(Blocks::never)
            .dynamicShape();
    protected AbstractPartBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(createBlock());
    }

    public abstract Function<Properties, AbstractPartBlock> createBlock();

    @Override
    public abstract @NotNull AbstractPartBlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState);

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext collisionContext) {
        if(level.getBlockEntity(pos) instanceof AbstractPartBlockEntity blockEntity)
            return blockEntity.getShape();
        return Shapes.block();
    }

    @Override
    protected int getLightBlock(BlockState p_60585_, BlockGetter p_60586_, BlockPos p_60587_) {
        return 0;
    }

    @Override
    protected boolean isPathfindable(BlockState p_60475_, PathComputationType p_60478_) {
        return false;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState state1, boolean p_60519_) {
        var be = level.getBlockEntity(pos);
        if(be instanceof AbstractPartBlockEntity partBlockEntity &&
                (!RagdollTypeRegistry.getRagdollTypeAbilities(partBlockEntity.getPartData().type()).fracture() ||
                        partBlockEntity.getPartData().isMain())){
            partBlockEntity.removeSelf();
        }
        super.onRemove(state, level, pos, state1, p_60519_);
    }
}
