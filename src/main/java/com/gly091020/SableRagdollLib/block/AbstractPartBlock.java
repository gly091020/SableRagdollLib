package com.gly091020.SableRagdollLib.block;

import com.gly091020.SableRagdollLib.api.RagdollTypeRegistry;
import com.mojang.serialization.MapCodec;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
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
        if(be instanceof AbstractPartBlockEntity partBlockEntity){
            partBlockEntity.removeSelf();
        }
        super.onRemove(state, level, pos, state1, p_60519_);

        if(!(level instanceof ServerLevel serverLevel))return;
        if(state.is(state1.getBlock()))return;
        if(SableCompanion.INSTANCE.getContaining(serverLevel, pos) == null){
            serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    @Override
    protected VoxelShape getVisualShape(BlockState p_60479_, BlockGetter p_60480_, BlockPos p_60481_, CollisionContext p_60482_) {
        return Shapes.empty();
    }

    @Override
    public void onBlockStateChange(LevelReader level, BlockPos pos, BlockState oldState, BlockState newState) {
        super.onBlockStateChange(level, pos, oldState, newState);
        if(!(level instanceof ServerLevel serverLevel))return;
        if(oldState.is(newState.getBlock()))return;
        if(SableCompanion.INSTANCE.getContaining(serverLevel, pos) == null){
            serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    @Override
    protected void onPlace(BlockState p_60566_, Level p_60567_, BlockPos p_60568_, BlockState p_60569_, boolean p_60570_) {
        super.onPlace(p_60566_, p_60567_, p_60568_, p_60569_, p_60570_);
    }
}
